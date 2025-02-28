package com.Soo_Shinsa.coupon.service;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.coupon.aop.CouponLock;
import com.Soo_Shinsa.coupon.dto.CouponBrandRelationDto;
import com.Soo_Shinsa.coupon.dto.CouponRequestDto;
import com.Soo_Shinsa.coupon.dto.CouponResponseDto;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponBrandRelation;
import com.Soo_Shinsa.coupon.model.CouponUser;
import com.Soo_Shinsa.coupon.repository.CouponRepository;
import com.Soo_Shinsa.coupon.repository.CouponUserRepository;
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


@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final BrandRepository brandRepository;
    private final CouponUserRepository couponUserRepository;

    @CouponLock(key = "'lock:coupon:' + #couponRequestDto.couponId")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public CouponResponseDto createCoupon(CouponRequestDto couponRequestDto, User user) {
        EntityValidator.validateAdminOrVendorAccess(user);

        // 1️⃣ 쿠폰 조회
        Coupon coupon = couponRepository.findById(couponRequestDto.getCouponId())
                .orElseGet(() -> {
                    log.info("📌 쿠폰이 존재하지 않음, 새 쿠폰 생성");
                    Coupon newCoupon = Coupon.builder()
                            .couponType(couponRequestDto.getCouponType())
                            .couponName(couponRequestDto.getCouponName())
                            .discountRate(couponRequestDto.getDiscountRate())
                            .maxCount(couponRequestDto.getMaxCount())
                            .build();
                    return couponRepository.save(newCoupon);
                });

        if (coupon.getIssuedCount() >= coupon.getMaxCount()) {
            log.error("❌ 쿠폰 수량 초과 - ID: {}", couponRequestDto.getCouponId());
            throw new InvalidInputException(ErrorCode.COUPON_OUT_OF_STOCK);
        }

        // 2️⃣ 중복 발급 방지
        if (couponUserRepository.existsByCouponAndUser(coupon, user)) {
            throw new InvalidInputException(ErrorCode.ALREADY_USED_COUPON);
        }

        // 3️⃣ 쿠폰 발급 개수 증가 (엔티티 내부에서 처리)
        coupon.issueCoupon();

        // 4️⃣ 브랜드의 쿠폰 수량 감소 (brand.getIsCouponLimited()가 true일 경우만)
        for (CouponBrandRelationDto relationDto : couponRequestDto.getBrands()) {
            Brand brand = brandRepository.findByIdOrElseThrow(relationDto.getBrandId());

            if (Boolean.TRUE.equals(brand.getIsCouponLimited())) {
                brand.decreaseCouponCount();
            }

            CouponBrandRelation relation = CouponBrandRelation.builder()
                    .coupon(coupon)
                    .brand(brand)
                    .build();
            coupon.getCouponBrandRelations().add(relation);
        }

        // 5️⃣ 사용자에게 쿠폰 발급
        issueCouponToUser(coupon, user);

        return CouponResponseDto.from(coupon);
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
