package com.Soo_Shinsa.category.dto;

import com.Soo_Shinsa.category.model.SubCategory;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubCategoryResponseDto {
    private Long id;
    private Long parentId;
    private String name;

    @Builder
    public SubCategoryResponseDto(Long id, Long parentId, String name) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
    }

    public static SubCategoryResponseDto toDto(SubCategory subCategory) {
        return SubCategoryResponseDto.builder()
                .id(subCategory.getId())
                .parentId(subCategory.getCategory().getId())
                .name(subCategory.getName())
                .build();
    }
}
