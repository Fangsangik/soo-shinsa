package com.Soo_Shinsa.statistics.repository;

import com.Soo_Shinsa.statistics.model.Statistics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatisticsRepository extends JpaRepository<Statistics, Long> {

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from Statistics s where s.orderDate = :date")
    int deleteByOrderDate(@org.springframework.data.repository.query.Param("date") java.time.LocalDate date);

    long countByOrderDate(java.time.LocalDate date);

}
