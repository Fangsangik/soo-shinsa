package com.Soo_Shinsa.product.dto;

import com.Soo_Shinsa.global.constant.ProductStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class FindProductRequestDto {
    private String nameKeyword;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private ProductStatus status;
    private Long categoryId;

    public FindProductRequestDto(String nameKeyword, BigDecimal minPrice, BigDecimal maxPrice, ProductStatus status, Long categoryId) {
        this.nameKeyword = nameKeyword;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.status = status;
        this.categoryId = categoryId;
    }
}
