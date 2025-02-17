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
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.utils.EntityValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final BrandRepository brandRepository;
    private final CouponUserRepository couponUserRepository;

    @CouponLock(key = "'lock:coupon:' + #couponRequestDto.couponId")
    @Transactional
    @Override
    public CouponResponseDto createCoupon(CouponRequestDto couponRequestDto, User user) {
        EntityValidator.validateAdminOrVendorAccess(user);
        // 쿠폰 조회
        Coupon coupon = couponRepository.findByIdForUpdate(couponRequestDto.getCouponId())
                .orElseGet(() -> {
                    // 쿠폰이 없을 경우 새로 생성
                    Coupon newCoupon = Coupon.builder()
                            .couponName(couponRequestDto.getCouponName())
                            .discountRate(couponRequestDto.getDiscountRate())
                            .couponType(couponRequestDto.getCouponType())
                            .maxCount(couponRequestDto.getMaxCount())
                            .build();
                    return couponRepository.save(newCoupon); // 새로 생성한 쿠폰 저장
                });

        boolean alreadyIssued = couponUserRepository.existsByCouponAndUser(coupon, user);
        if (alreadyIssued) {
            throw new IllegalStateException("사용자는 이미 이 쿠폰을 발급받았습니다.");
        }

        // 발급 수량 검증 및 증가 (원자적 처리)
        if (coupon.getIssuedCount() >= coupon.getMaxCount()) {
            throw new IllegalStateException("쿠폰 발급 한도를 초과했습니다.");
        }
        coupon.increaseIssuedCount(); // 발급 수 증가
        couponRepository.save(coupon);

        // 쿠폰 브랜드 연관 처리
        for (CouponBrandRelationDto relationDto : couponRequestDto.getBrands()) {
            Brand brand = brandRepository.findByIdOrElseThrow(relationDto.getBrandId());
            if (brand.getIsCouponLimited() != null && brand.getCouponCount() > 0) {
                brand.decreaseCouponCount();
                brandRepository.save(brand);
            }

            CouponBrandRelation relation = CouponBrandRelation.builder()
                    .coupon(coupon)
                    .brand(brand)
                    .build();
            coupon.getCouponBrandRelations().add(relation);
        }

        // 사용자에게 쿠폰 발급
        issueCouponToUser(coupon, user);

        // 응답 DTO 생성
        return CouponResponseDto.builder()
                .id(coupon.getId())
                .couponCode(coupon.getCouponCode())
                .couponName(coupon.getCouponName())
                .discountRate(coupon.getDiscountRate())
                .couponType(coupon.getCouponType())
                .issueDate(coupon.getIssueDate())
                .expirationDate(coupon.getExpirationDate())
                .maxCount(coupon.getMaxCount())
                .brandRelations(CouponBrandRelationDto.toDtos(coupon.getCouponBrandRelations()))
                .build();

    }

    @Transactional
    public void issueCouponToUser(Coupon coupon, User user) {
        CouponUser couponUser = CouponUser.builder()
                .coupon(coupon)
                .user(user)
                .isUsed(false)
                .usedAt(null)
                .build();

        couponUserRepository.save(couponUser);
    }
}
