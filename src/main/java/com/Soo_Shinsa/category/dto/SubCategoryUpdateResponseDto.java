package com.Soo_Shinsa.category.dto;

import com.Soo_Shinsa.category.model.SubCategory;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubCategoryUpdateResponseDto {
    private Long id;
    private Long parentId;
    private String name;

    @Builder
    public SubCategoryUpdateResponseDto(Long id, Long parentId, String name) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
    }

    public static SubCategoryUpdateResponseDto toDto(SubCategory findSubCategory) {
        return SubCategoryUpdateResponseDto.builder()
                .id(findSubCategory.getId())
                .parentId(findSubCategory.getCategory().getId())
                .name(findSubCategory.getName())
                .build();
    }
}
