package com.Soo_Shinsa.statistics.controller;

import com.Soo_Shinsa.statistics.dto.StatisticsForSaleRequestDto;
import com.Soo_Shinsa.statistics.dto.StatisticsResponseDto;
import com.Soo_Shinsa.statistics.service.StatisticsService;
import com.Soo_Shinsa.utils.CommonResponse;
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
    public ResponseEntity<CommonResponse<StatisticsResponseDto>> getStatisticsOfSales(@Valid @RequestBody StatisticsForSaleRequestDto requestDto) {
        // DTO 변환
        StatisticsResponseDto dto = statisticsService.getStatisticsOfSales(requestDto);
        return ResponseEntity.ok(new CommonResponse<>("통계 처리 성공", dto));
    }
}
