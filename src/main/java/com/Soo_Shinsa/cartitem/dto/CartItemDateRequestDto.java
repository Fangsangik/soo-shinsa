package com.Soo_Shinsa.cartitem.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Setter // @ModelAttribute 쿼리파라미터 바인딩용
@NoArgsConstructor
public class CartItemDateRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;

    public CartItemDateRequestDto(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
