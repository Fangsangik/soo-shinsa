package com.Soo_Shinsa.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class StatisticsBodyResponseDto {
    List<String> dataList;
}
