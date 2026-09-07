package com.Soo_Shinsa.coupon.service;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.coupon.dto.CouponBrandRelationDto;
import com.Soo_Shinsa.coupon.dto.CouponCreateRequestDto;
import com.Soo_Shinsa.coupon.dto.CouponResponseDto;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponBrandRelation;
import com.Soo_Shinsa.coupon.model.CouponUser;
import com.Soo_Shinsa.coupon.repository.CouponBrandRelationRepository;
import com.Soo_Shinsa.coupon.repository.CouponRepository;
import com.Soo_Shinsa.coupon.repository.CouponUserRepository;
import com.Soo_Shinsa.global.config.BusinessMetrics;
import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.Soo_Shinsa.global.utils.EntityValidator;
import com.Soo_Shinsa.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final BrandRepository brandRepository;
    private final CouponUserRepository couponUserRepository;
    private final CouponBrandRelationRepository couponBrandRelationRepository;
    private final CouponStockGuard couponStockGuard;
    private final BusinessMetrics metrics;

    /**
     * 쿠폰 정의 생성.
     *
     * 예전에는 발급 메서드가 "쿠폰이 없으면 만든다"까지 겸해서, 없는 id 로 동시에 요청이 오면
     * 쿠폰이 여러 개 생겼다. 생성은 관리자/업주의 별도 행위로 분리한다.
     * 브랜드 관계도 여기서 한 번만 만든다 (발급마다 만들면 발급 수만큼 중복 생성됐다).
     */
    @Transactional
    @Override
    public CouponResponseDto create(CouponCreateRequestDto requestDto, User user) {
        EntityValidator.validateAdminOrVendorAccess(user);

        Coupon coupon = couponRepository.save(Coupon.builder()
                .couponName(requestDto.getCouponName())
                .discountRate(requestDto.getDiscountRate())
                .couponType(requestDto.getCouponType())
                .maxCount(requestDto.getMaxCount())
                .build());

        List<CouponBrandRelationDto> brands = requestDto.getBrands();
        if (brands != null) {
            for (CouponBrandRelationDto relationDto : brands) {
                Brand brand = brandRepository.findByIdOrElseThrow(relationDto.getBrandId());
                CouponBrandRelation relation = CouponBrandRelation.builder()
                        .coupon(coupon)
                        .brand(brand)
                        .build();
                // 컬렉션에도 넣어야 응답 DTO 의 brandRelations 가 채워진다
                coupon.getCouponBrandRelations().add(relation);
                couponBrandRelationRepository.save(relation);
            }
        }

        // 정원이 새로 정해졌으므로 선차단 카운터를 버린다
        couponStockGuard.reset(coupon.getId());

        log.info("🎟 쿠폰 생성 - ID: {}, 정원: {}", coupon.getId(), coupon.getMaxCount());
        return CouponResponseDto.from(coupon);
    }

    /**
     * 선착순 쿠폰 발급.
     *
     * 분산락을 쓰지 않는다. 정원은 increaseIssuedCount 의 WHERE 조건이,
     * 1인 1매는 coupon_user 의 (coupon_id, user_id) unique 제약이 보장한다.
     * 둘 다 DB 가 원자적으로 처리하므로 락보다 정확하고 빠르다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public CouponResponseDto issue(Long couponId, User user) {
        Coupon coupon = couponRepository.findByIdOrElseThrow(couponId);

        // 1️⃣ Redis 선차단. 정원이 이미 찼으면 DB 를 건드리지 않고 여기서 끝낸다.
        if (!couponStockGuard.tryAcquire(couponId, coupon.getMaxCount() - coupon.getIssuedCount())) {
            log.info("🚫 선착순 마감 - ID: {}", couponId);
            metrics.couponPrefilterRejected();
            throw new InvalidInputException(ErrorCode.COUPON_OUT_OF_STOCK);
        }

        // 선점 후 실패하면 되돌려야 한다. 안 그러면 트랜잭션은 롤백되는데
        // Redis 카운터만 줄어든 채 남아 정원이 조용히 깎인다.
        try {
            // 2️⃣ 정원 안에서만 발급 수 증가. 0행이면 이미 소진.
            //    Redis 가 틀려도 여기서 막히므로 초과 발급은 불가능하다.
            //    영속성 컨텍스트를 비우므로 이후 엔티티는 다시 읽어서 쓴다.
            if (couponRepository.increaseIssuedCount(couponId) == 0) {
                log.error("❌ 쿠폰 수량 초과 - ID: {}", couponId);
                metrics.couponSoldOut();
                throw new InvalidInputException(ErrorCode.COUPON_OUT_OF_STOCK);
            }

            // 3️⃣ 브랜드별 쿠폰 잔여 수량 차감 (수량 제한이 걸린 브랜드만)
            for (CouponBrandRelation relation : couponBrandRelationRepository.findAllByCouponId(couponId)) {
                Brand brand = relation.getBrand();
                if (Boolean.TRUE.equals(brand.getIsCouponLimited())
                        && brandRepository.decreaseCouponCount(brand.getId()) == 0) {
                    log.error("❌ 브랜드 쿠폰 수량 소진 - 브랜드 ID: {}", brand.getId());
                    throw new InvalidInputException(ErrorCode.COUPON_OUT_OF_STOCK);
                }
            }

            // 4️⃣ 사용자에게 발급 (중복은 unique 제약이 막는다)
            Coupon issued = couponRepository.findByIdOrElseThrow(couponId);
            issueCouponToUser(issued, user);

            metrics.couponIssued();
            return CouponResponseDto.from(issued);
        } catch (RuntimeException e) {
            couponStockGuard.release(couponId);
            throw e;
        }
    }

    private void issueCouponToUser(Coupon coupon, User user) {
        try {
            CouponUser couponUser = CouponUser.builder()
                    .coupon(coupon)
                    .user(user)
                    .isUsed(false)
                    .usedAt(null)
                    .build();

            couponUserRepository.save(couponUser);
        } catch (DataIntegrityViolationException e) {
            log.error("❌ 쿠폰 중복 발급 시도 - couponId: {}, userId: {}", coupon.getId(), user.getUserId());
            throw new InvalidInputException(ErrorCode.ALREADY_USED_COUPON);
        }
    }
}
