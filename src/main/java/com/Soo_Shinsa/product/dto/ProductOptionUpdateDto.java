package com.Soo_Shinsa.product.dto;

import com.Soo_Shinsa.constant.ProductStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductOptionUpdateDto {
    private String size;
    private String color;
    private ProductStatus status;
    private Integer quantity;

    public ProductOptionUpdateDto(String size, String color, ProductStatus status, Integer quantity) {
        this.size = size;
        this.color = color;
        this.status = status;
        this.quantity = quantity;
    }
}
