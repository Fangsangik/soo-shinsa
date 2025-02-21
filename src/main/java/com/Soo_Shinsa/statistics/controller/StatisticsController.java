package com.Soo_Shinsa.statistics.controller;

import com.Soo_Shinsa.statistics.dto.StatisticsForSaleRequestDto;
import com.Soo_Shinsa.statistics.dto.StatisticsRequestDto;
import com.Soo_Shinsa.statistics.dto.StatisticsResponseDto;
import com.Soo_Shinsa.statistics.service.StatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService statisticsService;

    @GetMapping("/sales")
    public ResponseEntity<StatisticsResponseDto> getStatisticsOfSales(@Valid @RequestBody StatisticsForSaleRequestDto requestDto) {
        return ResponseEntity.ok(statisticsService.getStatisticsOfSales(requestDto));
    }
    @GetMapping("/count")
    public ResponseEntity<StatisticsResponseDto> getStatisticsOfCount(@Valid @RequestBody StatisticsRequestDto requestDto) {
        return ResponseEntity.ok(statisticsService.getStatisticsOfCount(requestDto));
    }
}
