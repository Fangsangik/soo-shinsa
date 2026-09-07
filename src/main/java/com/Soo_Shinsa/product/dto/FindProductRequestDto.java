package com.Soo_Shinsa.product.dto;

import com.Soo_Shinsa.global.constant.ProductStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter // @ModelAttribute 쿼리파라미터 바인딩용
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
