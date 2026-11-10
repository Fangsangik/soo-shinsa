package com.Soo_Shinsa.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ReviewRateDto {
    @Max(value = 5, message = "5점 이하여야 합니다")
    private Integer maxRate;

    @Min(value = 1, message = "1점 이상이어야 합니다")
    private Integer minRate;

    public ReviewRateDto(Integer maxRate, Integer minRate) {
        this.maxRate = maxRate;
        this.minRate = minRate;
    }
}
