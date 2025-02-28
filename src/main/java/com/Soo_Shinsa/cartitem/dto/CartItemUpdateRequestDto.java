package com.Soo_Shinsa.cartitem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CartItemUpdateRequestDto {

    private Long cartItemId;

    @NotNull(message = "상품 아이디를 입력해주세요.")
    private Long productId;

    @NotNull(message = "수량을 입력해주세요.")
    private Integer quantity;

    @NotNull(message = "상품 옵션 아이디를 입력해주세요.")
    private List<Long> productOptionIds; // ✅ 여러 개의 옵션 ID를 받을 수 있도록 변경

    public CartItemUpdateRequestDto(Long cartItemId, Long productId, Integer quantity, List<Long> productOptionIds) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.quantity = quantity;
        this.productOptionIds = productOptionIds;
    }
}
