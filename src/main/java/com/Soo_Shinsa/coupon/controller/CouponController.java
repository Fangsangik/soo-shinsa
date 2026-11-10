package com.Soo_Shinsa.coupon.controller;

import com.Soo_Shinsa.coupon.dto.CouponRequestDto;
import com.Soo_Shinsa.coupon.dto.CouponResponseDto;
import com.Soo_Shinsa.coupon.service.CouponService;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.utils.CommonResponse;
import com.Soo_Shinsa.utils.ResponseMessage;
import com.Soo_Shinsa.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<CommonResponse<CouponResponseDto>> createCoupon(@RequestBody CouponRequestDto requestDto,
                                                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = UserUtils.getUser(userDetails);
        CouponResponseDto coupon = couponService.createCoupon(requestDto, user);
        CommonResponse<CouponResponseDto> response = new CommonResponse<>(ResponseMessage.COUPON_CREATE_SUCCESS, coupon);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    /** 사용 가능한(미만료, 수량 남은) 쿠폰 목록 — 장바구니에서 선택용 */
    @org.springframework.web.bind.annotation.GetMapping
    public ResponseEntity<CommonResponse<java.util.List<CouponResponseDto>>> findAvailable() {
        return ResponseEntity.ok(new CommonResponse<>(ResponseMessage.COUPON_SELECT_SUCCESS, couponService.findAvailable()));
    }
}
