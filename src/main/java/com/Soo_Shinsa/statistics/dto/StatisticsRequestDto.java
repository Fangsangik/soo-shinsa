package com.Soo_Shinsa.statistics.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class StatisticsRequestDto {
    private LocalDate startDate;  // 조회할 데이터의 시작 날짜
    private LocalDate endDate;    // 조회할 데이터의 종료 날짜
    private List<String> categoryList; // 조회할 카테고리 목록
    private List<String> brandList;    // 조회할 브랜드 목록

    @Builder
    public StatisticsRequestDto(LocalDate startDate, LocalDate endDate, List<String> categoryList, List<String> brandList) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.categoryList = categoryList;
        this.brandList = brandList;
    }
}
