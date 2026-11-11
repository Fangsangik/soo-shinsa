package com.Soo_Shinsa.statistics.controller;

import com.Soo_Shinsa.statistics.service.StatisticsService;
import com.Soo_Shinsa.statistics.dto.StatisticsRequestDto;
import com.Soo_Shinsa.statistics.dto.StatisticsForSaleRequestDto;
import com.Soo_Shinsa.statistics.dto.StatisticsResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService statisticsService;
    private final com.Soo_Shinsa.statistics.service.StatisticsAggregateService aggregateService;

    /** 빈 문자열 날짜 파라미터(startDate=)를 null 로 바인딩 — MySQL "Incorrect DATE value" 방지 */
    @org.springframework.web.bind.annotation.InitBinder
    void trimEmptyToNull(org.springframework.web.bind.WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new org.springframework.beans.propertyeditors.StringTrimmerEditor(true));
    }

    /** 특정 날짜(기본 오늘) 주문을 즉시 집계. 야간 배치를 기다리지 않고 통계를 볼 때 사용. */
    @org.springframework.web.bind.annotation.PostMapping("/aggregate")
    public ResponseEntity<java.util.Map<String, Object>> aggregate(@org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        java.time.LocalDate target = date != null ? date : java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        long rows = aggregateService.aggregate(target);
        return ResponseEntity.ok(java.util.Map.of("date", target.toString(), "rows", rows));
    }

    @GetMapping("/sales")
    public ResponseEntity<StatisticsResponseDto> getStatisticsOfSales(@Valid @ModelAttribute StatisticsForSaleRequestDto requestDto) {
        return ResponseEntity.ok(statisticsService.getStatisticsOfSales(requestDto));
    }
    @GetMapping("/count")
    public ResponseEntity<StatisticsResponseDto> getStatisticsOfCount(@Valid @ModelAttribute StatisticsRequestDto requestDto) {
        return ResponseEntity.ok(statisticsService.getStatisticsOfCount(requestDto));
    }
}
