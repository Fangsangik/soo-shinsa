package com.Soo_Shinsa.wish.dto;

import com.Soo_Shinsa.constant.ProductStatus;
import com.Soo_Shinsa.wish.model.Wish;

import java.math.BigDecimal;

public record WishResponseDto(Long id, Long productId, String productName, BigDecimal price, String imageUrl, ProductStatus status, Long brandId, String brandName) {
    public static WishResponseDto from(Wish w) {
        var p = w.getProduct();
        return new WishResponseDto(w.getId(), p.getId(), p.getName(), p.getPrice(), p.getImageUrl(), p.getProductStatus(),
                p.getBrand() == null ? null : p.getBrand().getId(), p.getBrand() == null ? null : p.getBrand().getName());
    }
}
