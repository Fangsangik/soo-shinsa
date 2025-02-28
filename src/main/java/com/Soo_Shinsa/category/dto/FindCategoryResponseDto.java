package com.Soo_Shinsa.category.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FindCategoryResponseDto {
    private Long id;
    private String name;
    private Long totalCount; // 추가

    @Builder
    public FindCategoryResponseDto(Long id, String name, Long totalCount) { // 순서 맞추기
        this.id = id;
        this.name = name;
        this.totalCount = totalCount;
    }
}
