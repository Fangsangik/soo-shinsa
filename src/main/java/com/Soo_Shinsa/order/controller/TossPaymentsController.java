package com.Soo_Shinsa.order.controller;


import com.Soo_Shinsa.global.utils.UserUtils;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RequestMapping("/api")
@Controller
@Tag(name = "Payments API", description = "토스 결제 관련 API")
@RequiredArgsConstructor
public class TossPaymentsController {
    private final TossPaymentsService tossPaymentsService;


    @Value("${toss.client_api_key}")
    private String clientKey;


    // 결제 생성
    @PostMapping("/create")
    @Operation(summary = "결제 생성", description = "새로운 결제를 생성합니다.")
    public ResponseEntity<PaymentResponseDto> createPayment(@AuthenticationPrincipal UserDetails userDetails,
                                                            @Valid @RequestBody PaymentRequestDto requestDto) {
        User user = UserUtils.getUser(userDetails);
        PaymentResponseDto responseDto = tossPaymentsService.createPayment(requestDto, user);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @RequestMapping("/success")
    @Operation(summary = "결제 승인", description = "결제 완료 후 승인 처리합니다.")
    public String approvePayment(@RequestParam String paymentKey, @RequestParam String orderId, @RequestParam Long amount,
                                 Model model) throws JsonProcessingException {
        tossPaymentsService.approvePayment(paymentKey, orderId, amount, model);

        return "success";
    }

    @RequestMapping("/home/users/{userId}/orders/{orderId}")
    @Operation(summary = "결제 상세 조회", description = "특정 주문의 결제 정보를 조회합니다.")
    public String home(@PathVariable Long userId,
                       @PathVariable Long orderId,
                       Model model) {
        UserOrderDto item = tossPaymentsService.findItem(userId, orderId);
        BigDecimal totalPrice = item.getOrder().getTotalPrice();

        String orderName = item.getOrder().getOrderId();
        String name = item.getUser().getName();

        model.addAttribute("tosspayments_key", clientKey);
        System.out.println("clientKey = " + clientKey);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("orderName", orderName);
        model.addAttribute("name", name);
        return "home";

    }

    @PostMapping("/cancel")
    @Operation(summary = "결제 취소", description = "진행 중인 결제를 취소합니다.")
    public String cancelPayment(@RequestBody PaymentCancelDto dto,
                                @RequestParam String cancelReason
    ) throws JsonProcessingException {
        tossPaymentsService.cancelPayment(dto.getPaymentKey(), cancelReason);
        return "cancel";
    }

}
