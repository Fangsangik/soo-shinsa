package com.Soo_Shinsa.statistics.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class OrderHistoryForStatistic {
    private LocalDate orderDate;
    private int quantity;
    private BigDecimal price;
    private String productName;
    private String brandName;

    public OrderHistoryForStatistic(LocalDate orderDate, int quantity, BigDecimal price, String productName, String brandName) {
        this.orderDate = orderDate;
        this.quantity = quantity;
        this.price = price;
        this.productName = productName;
        this.brandName = brandName;
    }
}
