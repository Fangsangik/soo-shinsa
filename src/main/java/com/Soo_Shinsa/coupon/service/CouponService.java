package com.Soo_Shinsa.coupon.service;

import com.Soo_Shinsa.coupon.dto.CouponCreateRequestDto;
import com.Soo_Shinsa.coupon.dto.CouponResponseDto;
import com.Soo_Shinsa.user.model.User;

public interface CouponService {

    /** 쿠폰 정의를 만든다. 관리자/업주만. 발급은 하지 않는다. */
    CouponResponseDto create(CouponCreateRequestDto requestDto, User user);

    /** 이미 있는 쿠폰을 사용자에게 선착순 발급한다. 쿠폰을 새로 만들지 않는다. */
    CouponResponseDto issue(Long couponId, User user);
}
