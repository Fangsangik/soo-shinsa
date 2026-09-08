package com.Soo_Shinsa.wish.dto;

import com.Soo_Shinsa.wish.model.Wish;

import java.math.BigDecimal;

public record WishResponseDto(
        Long id,
        Long productId,
        String productName,
        BigDecimal price,
        String brandName
) {
    public static WishResponseDto from(Wish wish) {
        return new WishResponseDto(
                wish.getId(),
                wish.getProduct().getId(),
                wish.getProduct().getName(),
                wish.getProduct().getPrice(),
                wish.getProduct().getBrand() == null ? null : wish.getProduct().getBrand().getName());
    }
}
