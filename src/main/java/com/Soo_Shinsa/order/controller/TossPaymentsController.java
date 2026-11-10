package com.Soo_Shinsa.order.controller;


import com.Soo_Shinsa.order.dto.PaymentCancelDto;
import com.Soo_Shinsa.order.dto.PaymentRequestDto;
import com.Soo_Shinsa.order.dto.PaymentResponseDto;
import com.Soo_Shinsa.order.dto.UserOrderDTO;
import com.Soo_Shinsa.order.service.TossPaymentsService;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.utils.UserUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
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
@RequiredArgsConstructor
public class TossPaymentsController {
    private final TossPaymentsService tossPaymentsService;


    @Value("${toss.client-key}")
    private String clientKey;


    // 결제 생성
    @PostMapping("/create")
    public ResponseEntity<PaymentResponseDto> createPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid
            @RequestBody PaymentRequestDto requestDto) {
        User user = UserUtils.getUser(userDetails);
        PaymentResponseDto responseDto = tossPaymentsService.createPayment(requestDto,user);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @RequestMapping("/success")
    public String approvePayment (
            @RequestParam String paymentKey, @RequestParam String orderId, @RequestParam Long amount,
            Model model) throws JsonProcessingException {
        tossPaymentsService.approvePayment(paymentKey,orderId,amount,model);

        return "success";
    }

    /** 프론트(SPA #/pay/:id) 결제 페이지용 공개 클라이언트 키. 주문 정보는 인증된 /orders/{id}로 조회한다. */
    @GetMapping("/client-key")
    @ResponseBody
    public java.util.Map<String, String> clientKey() {
        return java.util.Map.of("clientKey", clientKey);
    }

    @GetMapping("/fail")
    public String failPayment(@RequestParam(required = false) String code,
                              @RequestParam(required = false) String message, Model model) {
        model.addAttribute("code", code);
        model.addAttribute("message", message);
        return "fail";
    }

    @PostMapping("/cancel")
    public String cancelPayment (
            @RequestBody PaymentCancelDto dto,
            @RequestParam String cancelReason
    ) throws JsonProcessingException {
        tossPaymentsService.cancelPayment(dto.getPaymentKey(), cancelReason);
        return "cancel";
    }

}
