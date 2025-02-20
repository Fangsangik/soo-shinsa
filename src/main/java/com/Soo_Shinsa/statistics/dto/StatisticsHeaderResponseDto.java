package com.Soo_Shinsa.statistics.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class StatisticsHeaderResponseDto {
    private List<String> dateList;
    private List<String> brandList;

    public StatisticsHeaderResponseDto(List<String> dateList, List<String> brandList) {
        this.dateList = dateList;
        this.brandList = brandList;
    }
}
