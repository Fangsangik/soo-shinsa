package com.Soo_Shinsa.order.dto;

import java.math.BigDecimal;

/**
 * 토스 결제 위젯을 띄우는 데 필요한 값만 담는다.
 *
 * 예전에는 이 자리에서 User/Orders 엔티티를 통째로 넘기는 UserOrderDto 를 썼는데,
 * JSON 으로 나가면 비밀번호 해시까지 딸려 나간다.
 */
public record CheckoutResponseDto(
        String clientKey,
        String orderId,
        String orderName,
        BigDecimal amount,
        String customerName
) {
}
