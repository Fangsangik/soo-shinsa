package com.Soo_Shinsa.product.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class FindProductOptionRequestDto {

    private String size;
    private String color;


    public FindProductOptionRequestDto(String size, String color) {
        this.size = size;
        this.color = color;
    }
}
