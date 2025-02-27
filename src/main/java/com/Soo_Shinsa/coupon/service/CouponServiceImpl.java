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
import com.Soo_Shinsa.exception.ErrorCode;
import com.Soo_Shinsa.exception.InvalidInputException;
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
//        Coupon coupon = couponRepository.existsById(couponRequestDto.getCouponId());
//        Coupon coupon = couponRepository.findByIdForUpdate(couponRequestDto.getCouponId())
//                .orElseThrow(()-> new InvalidInputException(ErrorCode.ALREADY_USED_COUPON));

        Coupon coupon = Coupon.builder()
                .discountRate(couponRequestDto.getDiscountRate())
                .couponName(couponRequestDto.getCouponName())
                .couponType(couponRequestDto.getCouponType())
                .maxCount(couponRequestDto.getMaxCount())
                .isUsed(false)
                .build();

        couponRepository.save(coupon);

        if (coupon.getIssuedCount() >= coupon.getMaxCount()) {
            throw new InvalidInputException(ErrorCode.COUPON_OUT_OF_STOCK);
        }

        boolean alreadyIssued = couponUserRepository.existsByCouponAndUser(coupon, user);
        if (alreadyIssued) {
            throw new InvalidInputException(ErrorCode.ALREADY_USED_COUPON);
        }

        int count = couponRepository.incrementIssuedCount(coupon.getId());
        if (count == 0) {
            throw new InvalidInputException(ErrorCode.COUPON_OUT_OF_STOCK);
        }

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
        // 쿠폰 브랜드 연관 처리

        // 사용자에게 쿠폰 발급
        issueCouponToUser(coupon, user);

        // 응답 DTO 생성
        return CouponResponseDto.from(coupon);

    }


    private void issueCouponToUser(Coupon coupon, User user) {
        CouponUser couponUser = CouponUser.builder()
                .coupon(coupon)
                .user(user)
                .isUsed(false)
                .usedAt(null)
                .build();

        couponUserRepository.save(couponUser);
    }
}
