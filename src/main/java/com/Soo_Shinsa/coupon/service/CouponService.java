package com.Soo_Shinsa.coupon.service;

import com.Soo_Shinsa.coupon.dto.CouponRequestDto;
import com.Soo_Shinsa.coupon.dto.CouponResponseDto;
import com.Soo_Shinsa.user.model.User;

public interface CouponService {
    CouponResponseDto createCoupon(CouponRequestDto couponRequestDto, User user);
    /** 미만료·수량 남음·이 사용자가 아직 쓰지 않은 쿠폰 */
    java.util.List<CouponResponseDto> findAvailable(User user);

    java.util.List<com.Soo_Shinsa.coupon.dto.MyCouponResponseDto> findMine(User user);
}
