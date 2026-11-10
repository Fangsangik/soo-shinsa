package com.Soo_Shinsa.coupon.service;

import com.Soo_Shinsa.coupon.dto.CouponRequestDto;
import com.Soo_Shinsa.coupon.dto.CouponResponseDto;
import com.Soo_Shinsa.user.model.User;

public interface CouponService {
    CouponResponseDto createCoupon(CouponRequestDto couponRequestDto, User user);
    java.util.List<CouponResponseDto> findAvailable();
}
