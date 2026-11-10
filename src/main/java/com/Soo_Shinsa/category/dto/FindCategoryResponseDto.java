package com.Soo_Shinsa.category.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FindCategoryResponseDto {

    private Long id;

    private Long parentId;

    private String name;

    private Long totalCount;

    public FindCategoryResponseDto(Long id, String name, Long totalCount) {
        this.id = id;
        this.name = name;
        this.totalCount = totalCount;
    }

    @Builder
    public FindCategoryResponseDto(Long id, Long subCategoryId, Long parentId, String name, Long totalCount) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.totalCount = totalCount;
    }
}
