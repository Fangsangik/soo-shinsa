package com.Soo_Shinsa.statistics.repository;

import com.Soo_Shinsa.statistics.model.Statistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StatisticsRepository extends JpaRepository<Statistics, Long> {

    @Query("SELECT s.brandName, SUM(s.price * s.quantity) " +
            "FROM Statistics s " +
            "WHERE s.orderDate BETWEEN :startDate AND :endDate " +
            "AND (:categoryList IS NULL OR s.productName IN :categoryList) " +
            "AND (:brandList IS NULL OR s.brandName IN :brandList) " +
            "GROUP BY s.brandName")
    List<Object[]> findSalesByCriteriaAsList(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("categoryList") List<String> categoryList,
                                             @Param("brandList") List<String> brandList);
}
