package com.Soo_Shinsa.statistics.service;

import com.Soo_Shinsa.global.constant.StatisticsEnum;
import com.Soo_Shinsa.statistics.dto.StatisticsForSaleRequestDto;
import com.Soo_Shinsa.statistics.dto.StatisticsHeaderResponseDto;
import com.Soo_Shinsa.statistics.dto.StatisticsRequestDto;
import com.Soo_Shinsa.statistics.dto.StatisticsResponseDto;
import com.Soo_Shinsa.statistics.repository.StatisticsMybatisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final StatisticsMybatisRepository repository;


    @Override
    public StatisticsResponseDto getStatisticsOfSales(StatisticsForSaleRequestDto requestDto) {
        StatisticsResponseDto dto = new StatisticsResponseDto();

        StatisticsRequestDto statisticsRequestDto = requestDto.toStatisticsRequestDto();

        List<String> dateList = repository.getDateList(statisticsRequestDto);
        List<String> brandList = repository.getBrandList(statisticsRequestDto);
        StatisticsHeaderResponseDto statisticsHeaderResponseDto = new StatisticsHeaderResponseDto(dateList, brandList);

        for (String brand : brandList) {
            List<String> dataList = repository.getBodyDataListForSales(statisticsRequestDto, brand)
                    .stream()
                    .map(data -> {
                        if (requestDto.getStartPrice() != null && requestDto.getEndPrice() != null) {
                            return data.compareTo(requestDto.getStartPrice()) > 0 && data.compareTo(requestDto.getEndPrice()) < 0 ? data : BigDecimal.ZERO;
                        } else if (requestDto.getStartPrice() != null) {
                            return data.compareTo(requestDto.getStartPrice()) > 0 ? data : BigDecimal.ZERO;
                        } else if (requestDto.getEndPrice() != null ) {
                            return data.compareTo(requestDto.getEndPrice()) < 0 ? data : BigDecimal.ZERO;
                        }else {
                            return data;
                        }
                    })
                    .map(BigDecimal::toString)
                    .toList();
            dto.addBodyData(dataList);
        }

        //총합 추가
        brandList.add(StatisticsEnum.TOTAL.getName());
        dto.addHeaderData(statisticsHeaderResponseDto);
        dto.addBodyData(repository.getBodyDataListForSales(statisticsRequestDto, null)
                .stream()
                .map(BigDecimal::toString)
                .toList());

        return dto;

    }

    @Override
    public StatisticsResponseDto getStatisticsOfCount(StatisticsRequestDto requestDto) {

        StatisticsResponseDto dto = new StatisticsResponseDto();

        List<String> dateList = repository.getDateList(requestDto);
        List<String> brandList = repository.getBrandList(requestDto);

        StatisticsHeaderResponseDto statisticsHeaderResponseDto = new StatisticsHeaderResponseDto(dateList, brandList);

        for (String brand : brandList) {
            dto.addBodyData(repository.getBodyDataListForCount(requestDto, brand));


        }

        //총합 추가
        brandList.add(StatisticsEnum.TOTAL.getName());
        dto.addHeaderData(statisticsHeaderResponseDto);
        dto.addBodyData(repository.getBodyDataListForCount(requestDto, null));

        return dto;
    }
}
