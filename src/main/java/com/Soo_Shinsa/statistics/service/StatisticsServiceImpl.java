package com.Soo_Shinsa.statistics.service;

import com.Soo_Shinsa.statistics.dto.StatisticsForSaleRequestDto;
import com.Soo_Shinsa.statistics.dto.StatisticsHeaderResponseDto;
import com.Soo_Shinsa.statistics.dto.StatisticsResponseDto;
import com.Soo_Shinsa.statistics.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final StatisticsRepository repository;


    /**
     * 판매 통계
     * @param requestDto
     * @return
     */
    @Override
    public StatisticsResponseDto getStatisticsOfSales(StatisticsForSaleRequestDto requestDto) {
        /**
         * startDate, endDate, categoryList, brandList, orderStatus을 기준으로 데이터를 필터링.
         */
        List<Object[]> statisticsList = repository.findSalesByCriteriaAsList(
                LocalDate.parse(requestDto.getStartDate()),
                LocalDate.parse(requestDto.getEndDate()),
                requestDto.getCategoryList(),
                requestDto.getBrandList()
        );

        /**
         * 조회된 데이터(List<Object[]>)를 Map<String, BigDecimal> 형태로 변환.
         */
        Map<String, BigDecimal> statistics = statisticsList.stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (BigDecimal) row[1]));

        /**
         * StatisticsResponseDto 객체를 생성하여 응답을 준비.
         * StatisticsHeaderResponseDto를 생성하여 헤더 정보(브랜드명 리스트) 추가.
         */
        // DTO 변환
        StatisticsResponseDto dto = new StatisticsResponseDto();
        StatisticsHeaderResponseDto headerDto = new StatisticsHeaderResponseDto(
                statistics.keySet().stream().toList(), statistics.keySet().stream().toList()
        );

        dto.addHeaderData(headerDto);
        dto.addBodyData(statistics.values().stream().map(BigDecimal::toString).toList());

        return dto;
    }
}
