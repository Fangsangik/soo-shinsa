package com.Soo_Shinsa.product.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SingleProductOrderRequestDto {
    // 서비스는 이 값으로 ProductOption 을 찾는다. 이름이 productId 라서
    // 상품 id 를 보내면 "상품옵션을 찾을 수 없습니다"가 났다.
    @NotNull(message = "상품 옵션 Id는 필수값 입니다.")
    private Long productOptionId;
    @NotNull(message = "수량은 필수값 입니다.")
    private Integer quantity;
}