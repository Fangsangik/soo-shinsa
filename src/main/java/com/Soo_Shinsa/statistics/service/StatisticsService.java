package com.Soo_Shinsa.statistics.service;

import com.Soo_Shinsa.statistics.dto.StatisticsForSaleRequestDto;
import com.Soo_Shinsa.statistics.dto.StatisticsResponseDto;

public interface StatisticsService {
   StatisticsResponseDto getStatisticsOfSales(StatisticsForSaleRequestDto requestDto);
}
