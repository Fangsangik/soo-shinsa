package com.Soo_Shinsa.statistics.model;

import com.Soo_Shinsa.statistics.dto.OrderHistoryForStatistic;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class Statistics {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "statistics_id")
    private Long statisticsId;

    //제품 관련 정보
    private String productName;
    private BigDecimal totalPrice;
    private String categoryName;
    private String brandName;

    private Integer quantity;

    //주문 관련 정보
    private LocalDate orderDate;
    private String orderStatus;

    //구매자 관련 정보


    public Statistics(OrderHistoryForStatistic orderHistory) {
        this.productName = orderHistory.getProductName();
        this.totalPrice = orderHistory.getPrice();
        this.categoryName = null;
        this.brandName = orderHistory.getBrandName();

        this.quantity = orderHistory.getQuantity();
        this.orderDate = orderHistory.getOrderDate().toLocalDateTime().toLocalDate();
        this.orderStatus = orderHistory.getOrderStatus();
    }


}
