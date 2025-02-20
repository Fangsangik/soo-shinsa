package com.Soo_Shinsa.statistics.model;

import com.Soo_Shinsa.constant.OrdersStatus;
import com.Soo_Shinsa.statistics.dto.OrderHistoryForStatistic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class Statistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String brandName;
    private String productName;
    private BigDecimal price;
    private int quantity;
    private LocalDate orderDate;

    // 기존 생성자가 있다면 유지


    public Statistics(String brandName, String productName, BigDecimal price, int quantity, LocalDate orderDate, OrdersStatus ordersStatus) {
        this.brandName = brandName;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.orderDate = orderDate;
    }

    public Statistics(OrderHistoryForStatistic orderHistoryForStatistic) {
        this.brandName = orderHistoryForStatistic.getBrandName();
        this.productName = orderHistoryForStatistic.getProductName();
        this.price = orderHistoryForStatistic.getPrice();
        this.quantity = orderHistoryForStatistic.getQuantity();
        this.orderDate = orderHistoryForStatistic.getOrderDate();
    }
}
