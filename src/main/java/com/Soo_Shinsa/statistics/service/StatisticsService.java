package com.Soo_Shinsa.statistics.service;

import com.Soo_Shinsa.statistics.dto.StatisticsForSaleRequestDto;
import com.Soo_Shinsa.statistics.dto.StatisticsRequestDto;
import com.Soo_Shinsa.statistics.dto.StatisticsResponseDto;
import jakarta.validation.Valid;

public interface StatisticsService {
    StatisticsResponseDto getStatisticsOfSales(@Valid StatisticsForSaleRequestDto requestDto);

    StatisticsResponseDto getStatisticsOfCount(@Valid StatisticsRequestDto requestDto);
}
