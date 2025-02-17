package com.Soo_Shinsa.category.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubCategoryUpdateRequestDto {

    private String name;

    @Builder
    public SubCategoryUpdateRequestDto(String name) {
        this.name = name;
    }
}
