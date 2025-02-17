package com.Soo_Shinsa.category.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubCategoryRequestDto {

    @NotNull(message = "상위 카테고리를 선택해주세요.")
    private Long parentId;
    @NotEmpty(message = "카테고리 이름을 입력해주세요.")
    private String name;

    public SubCategoryRequestDto(Long parentId, String name) {
        this.parentId = parentId;
        this.name = name;
    }
}
