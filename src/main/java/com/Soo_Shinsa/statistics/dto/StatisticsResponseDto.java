package com.Soo_Shinsa.statistics.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
public class StatisticsResponseDto {
    StatisticsHeaderResponseDto headerData;
    List<StatisticsBodyResponseDto> bodyDataList = new ArrayList<>();

    public void addBodyData(List<String> dataList) {
        this.bodyDataList.add(new StatisticsBodyResponseDto(dataList));
    }
    public void addHeaderData(StatisticsHeaderResponseDto dto) {
        this.headerData = dto;
    }
}
