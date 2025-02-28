package com.Soo_Shinsa.product.dto;

import com.Soo_Shinsa.global.constant.ProductStatus;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ProductOptionRequestDto {

    @NotEmpty(message = "사이즈는 필수 값 입니다.")
    private String size;

    @NotEmpty(message = "색상은 필수 값 입니다.")
    private String color;

    @NotNull(message = "상태는 필수 값 입니다.")
    private ProductStatus status;

    @NotNull(message = "수량은 필수 값 입니다.")
    private Integer quantity;


    public ProductOptionRequestDto(String size, String color, ProductStatus status, Integer quantity) {
        this.size = size;
        this.color = color;
        this.status = status;
        this.quantity = quantity;
    }

    public ProductOption toEntity(Product findProduct) {
        return ProductOption.builder()
                .size(size)
                .color(color)
                .productStatus(status)
                .product(findProduct)
                .quantity(quantity)
                .build();
    }
}