package com.Soo_Shinsa.coupon.controller;

import com.Soo_Shinsa.coupon.dto.CouponCreateRequestDto;
import com.Soo_Shinsa.coupon.dto.CouponResponseDto;
import com.Soo_Shinsa.coupon.service.CouponService;
import com.Soo_Shinsa.global.utils.CommonResponse;
import com.Soo_Shinsa.global.utils.ResponseMessage;
import com.Soo_Shinsa.global.utils.UserUtils;
import com.Soo_Shinsa.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
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
    @Operation(summary = "쿠폰 생성", description = "관리자/업주가 쿠폰을 정의합니다. 발급은 하지 않습니다.")
    public ResponseEntity<CommonResponse<CouponResponseDto>> createCoupon(
            @Valid @RequestBody CouponCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = UserUtils.getUser(userDetails);
        CouponResponseDto coupon = couponService.create(requestDto, user);
        CommonResponse<CouponResponseDto> response = new CommonResponse<>(ResponseMessage.COUPON_CREATE_SUCCESS, coupon);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{couponId}/issue")
    @Operation(summary = "쿠폰 발급", description = "이미 만들어진 쿠폰을 선착순으로 발급받습니다.")
    public ResponseEntity<CommonResponse<CouponResponseDto>> issueCoupon(
            @PathVariable Long couponId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = UserUtils.getUser(userDetails);
        CouponResponseDto coupon = couponService.issue(couponId, user);
        CommonResponse<CouponResponseDto> response = new CommonResponse<>(ResponseMessage.COUPON_ISSUE_SUCCESS, coupon);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
