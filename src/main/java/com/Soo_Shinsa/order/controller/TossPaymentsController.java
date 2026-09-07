package com.Soo_Shinsa.order.controller;


import com.Soo_Shinsa.global.utils.UserUtils;
import com.Soo_Shinsa.order.dto.CheckoutResponseDto;
import com.Soo_Shinsa.order.dto.PaymentCancelDto;
import com.Soo_Shinsa.order.dto.PaymentRequestDto;
import com.Soo_Shinsa.order.dto.PaymentResponseDto;
import com.Soo_Shinsa.order.dto.UserOrderDto;
import com.Soo_Shinsa.order.service.TossPaymentsService;
import com.Soo_Shinsa.user.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 결제 API. ApiPathConfig 가 /api/v1 을 붙이므로 실제 경로는 /api/v1/payments/** 다.
 *
 * 결제사가 브라우저를 돌려보내는 콜백(/api/success, /api/fail)은 TossCallbackController 에 있다.
 */
@RequestMapping("/payments")
@RestController
@Tag(name = "Payments API", description = "토스 결제 관련 API")
@RequiredArgsConstructor
public class TossPaymentsController {

    private final TossPaymentsService tossPaymentsService;

    @Value("${toss.client_api_key}")
    private String clientKey;

    @PostMapping
    @Operation(summary = "결제 생성", description = "새로운 결제를 생성합니다.")
    public ResponseEntity<PaymentResponseDto> createPayment(@AuthenticationPrincipal UserDetails userDetails,
                                                            @Valid @RequestBody PaymentRequestDto requestDto) {
        User user = UserUtils.getUser(userDetails);
        PaymentResponseDto responseDto = tossPaymentsService.createPayment(requestDto, user);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * 결제 위젯을 띄우는 데 필요한 값.
     *
     * 예전에는 /api/home/users/{userId}/orders/{orderId} 였는데, 경로의 userId 는 어차피
     * 로그인한 사용자여야 하므로 받지 않는다. (게다가 이 경로는 JwtAuthFilter 화이트리스트에
     * 걸려 토큰을 읽지 않았고, 그래서 항상 401 이었다.)
     */
    @GetMapping("/checkout/{orderId}")
    @Operation(summary = "결제 준비 정보", description = "결제 위젯에 필요한 주문 정보를 반환합니다.")
    public CheckoutResponseDto checkout(@PathVariable Long orderId,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        User user = UserUtils.getUser(userDetails);
        UserOrderDto item = tossPaymentsService.findItem(user.getUserId(), orderId, user);
        return new CheckoutResponseDto(
                clientKey,
                item.getOrder().getOrderId(),
                item.getOrder().getOrderId(),
                item.getOrder().getTotalPrice(),
                item.getUser().getName());
    }

    @PostMapping("/cancel")
    @Operation(summary = "결제 취소", description = "진행 중인 결제를 취소합니다.")
    public ResponseEntity<Void> cancelPayment(@RequestBody PaymentCancelDto dto,
                                              @RequestParam String cancelReason,
                                              @AuthenticationPrincipal UserDetails userDetails
    ) throws JsonProcessingException {
        tossPaymentsService.cancelPayment(dto.getPaymentKey(), cancelReason, UserUtils.getUser(userDetails));
        return ResponseEntity.noContent().build();
    }

}
