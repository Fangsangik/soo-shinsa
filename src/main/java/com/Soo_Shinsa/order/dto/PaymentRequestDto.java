package com.Soo_Shinsa.order.dto;

import com.Soo_Shinsa.global.constant.TossPayMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class PaymentRequestDto {
    private String paymentKey; // 토스페이먼츠 결제 키
    @NotNull(message = "결제방법을 입력해주세요.")
    private TossPayMethod method; // 결제 수단 (CARD)

    @NotNull(message = "오더는 필수값 입니다.")
    private Long order; // 주문 ID





    public PaymentRequestDto(String paymentKey, TossPayMethod method,Long order) {
        this.paymentKey = paymentKey;
        this.method = method;
        this.order=order;

    }
}