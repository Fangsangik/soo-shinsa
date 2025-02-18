package com.Soo_Shinsa.coupon.controller;

import com.Soo_Shinsa.coupon.dto.CouponRequestDto;
import com.Soo_Shinsa.coupon.dto.CouponResponseDto;
import com.Soo_Shinsa.coupon.service.CouponService;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.utils.CommonResponse;
import com.Soo_Shinsa.utils.ResponseMessage;
import com.Soo_Shinsa.utils.UserUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Coupon API", description = "쿠폰 관련 API")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    @Operation(summary = "쿠폰 생성", description = "새로운 쿠폰을 생성합니다.")
    public ResponseEntity<CommonResponse<CouponResponseDto>> createCoupon(@RequestBody CouponRequestDto requestDto,
                                                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = UserUtils.getUser(userDetails);
        CouponResponseDto coupon = couponService.createCoupon(requestDto, user);
        CommonResponse<CouponResponseDto> response = new CommonResponse<>(ResponseMessage.COUPON_CREATE_SUCCESS, coupon);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
