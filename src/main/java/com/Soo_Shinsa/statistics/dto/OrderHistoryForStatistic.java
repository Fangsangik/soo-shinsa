package com.Soo_Shinsa.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@NoArgsConstructor
@Getter
@AllArgsConstructor
public class OrderHistoryForStatistic {

    private String orderStatus;
    private Timestamp orderDate;

    private Integer quantity;
    private BigDecimal price;

    private String productName;
    private String categoryName;
    private String brandName;


}
