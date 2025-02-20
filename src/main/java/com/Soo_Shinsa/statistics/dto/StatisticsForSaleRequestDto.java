package com.Soo_Shinsa.statistics.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class StatisticsForSaleRequestDto {
    private String startDate;
    private String endDate;
    private List<String> categoryList;
    private List<String> brandList;

    @Builder
    public StatisticsForSaleRequestDto(String startDate, String endDate, List<String> categoryList, List<String> brandList, String orderStatus) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.categoryList = categoryList;
        this.brandList = brandList;
    }

    /**
     * `StatisticsForSaleRequestDto` 객체를 `StatisticsRequestDto`로 변환
     */
    public StatisticsRequestDto toStatisticsRequestDto() {
        return StatisticsRequestDto.builder()
                .startDate(LocalDate.parse(this.startDate))
                .endDate(LocalDate.parse(this.endDate))
                .categoryList(this.categoryList)
                .brandList(this.brandList)
                .build();
    }
}
