package com.Soo_Shinsa.statistics.dto;

import com.Soo_Shinsa.constant.PeriodType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsRequestDto {

    //출력 종류
    //연간/월간/주간/일간
    @NotNull(message = "기간 타입은 필수 입력 값입니다.")
    private PeriodType periodType;

    //검색 조건
    private List<String> categoryList;
    private List<String> brandList;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private String startDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private String endDate;

    private String orderStatus;


    public StatisticsRequestDto(PeriodType periodType, List<String> categoryList, List<String> brandList, String startDate, String endDate) {
        this.periodType = periodType;
        this.categoryList = categoryList;
        this.brandList = brandList;
        this.startDate = startDate;
        this.endDate = endDate;
    }

}
