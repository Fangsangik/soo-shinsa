package com.Soo_Shinsa.statistics.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class StatisticsResponseDto {

    private StatisticsHeaderResponseDto headerData;
    private List<String> bodyData;

    public void addHeaderData(StatisticsHeaderResponseDto headerData) {
        this.headerData = headerData;
    }

    public void addBodyData(List<String> bodyData) {
        this.bodyData = bodyData;
    }
}
