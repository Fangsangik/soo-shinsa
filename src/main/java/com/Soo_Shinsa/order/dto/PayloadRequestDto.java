package com.Soo_Shinsa.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PayloadRequestDto {
    private String orderId;
    private String amount;
    private String cancelReason;

    public PayloadRequestDto(String orderId, String amount) {
        this.orderId = orderId;
        this.amount = amount;
    }

    public PayloadRequestDto(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
