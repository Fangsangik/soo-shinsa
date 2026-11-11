package com.Soo_Shinsa.statistics.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** 배치 리더의 네이티브 쿼리 결과. Hibernate 6 는 select 컬럼 순서/개수와 정확히 일치하는 생성자 하나만 허용한다 (BatchConfig 쿼리와 함께 수정할 것). */
@Getter
public class OrderHistoryForStatistic {

    private final String orderStatus;
    private final Timestamp orderDate;
    private final Integer quantity;
    private final BigDecimal price;      // 라인 매출 = (할인가 또는 단가) × 수량
    private final String productName;
    private final String categoryName;
    private final String brandName;

    public OrderHistoryForStatistic(String orderStatus, Timestamp orderDate, Integer quantity, BigDecimal price, String productName, String categoryName, String brandName) {
        this.orderStatus = orderStatus;
        this.orderDate = orderDate;
        this.quantity = quantity;
        this.price = price;
        this.productName = productName;
        this.categoryName = categoryName;
        this.brandName = brandName;
    }
}
