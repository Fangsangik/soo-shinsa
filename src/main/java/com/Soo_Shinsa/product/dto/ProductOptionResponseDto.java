package com.Soo_Shinsa.product.dto;

import com.Soo_Shinsa.constant.ProductStatus;
import com.Soo_Shinsa.product.model.ProductOption;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProductOptionResponseDto {

    private Long id;
    private String size;
    private String color;
    private ProductStatus status;
    private Long product;
    private Integer quantity;

    @Builder
    public ProductOptionResponseDto(Long id, String size, String color, ProductStatus status, Long product, Integer quantity) {
        this.id = id;
        this.size = size;
        this.color = color;
        this.status = status;
        this.product = product;
        this.quantity = quantity;
    }

    public static ProductOptionResponseDto toDto(ProductOption savedOption) {
        return ProductOptionResponseDto.builder()
                .id(savedOption.getId())
                .color(savedOption.getColor())
                .product(savedOption.getProduct().getId())
                .status(ProductStatus.AVAILABLE)
                .size(savedOption.getSize())
                .quantity(savedOption.getQuantity())
                .build();
    }
}
