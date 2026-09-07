package com.Soo_Shinsa.order.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Setter // @ModelAttribute 쿼리파라미터 바인딩용
@NoArgsConstructor
public class OrderDateRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;

    public OrderDateRequestDto(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
