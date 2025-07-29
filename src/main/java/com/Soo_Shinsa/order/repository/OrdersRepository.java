package com.Soo_Shinsa.order.repository;

import com.Soo_Shinsa.global.exception.NotFoundException;
import com.Soo_Shinsa.order.dto.OrderSummaryDto;
import com.Soo_Shinsa.order.model.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.Soo_Shinsa.global.exception.ErrorCode.NOT_FOUND_ORDER;

public interface OrdersRepository extends JpaRepository<Orders, Long>, OrderCustomRepository {

    default Orders findByIdOrElseThrow(Long orderId) {
        return findById(orderId).orElseThrow(
                () -> new NotFoundException(NOT_FOUND_ORDER));
    }
    
    Optional<Orders> findByOrderId(String orderId);

    // 🚀 성능 최적화: N+1 문제 해결을 위한 Fetch Join
    @Query("SELECT DISTINCT o FROM Orders o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product p " +
           "LEFT JOIN FETCH oi.productOption po " +
           "WHERE o.id = :orderId")
    Optional<Orders> findByIdWithItems(@Param("orderId") Long orderId);

    @Query("SELECT DISTINCT o FROM Orders o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product p " +
           "LEFT JOIN FETCH oi.productOption po " +
           "WHERE o.id = :orderId AND o.user.userId = :userId")
    Optional<Orders> findByIdWithItemsAndUser(@Param("orderId") Long orderId, @Param("userId") Long userId);

    // 🚀 성능 최적화: 페이징을 위한 2단계 조회
    @Query("SELECT o.id FROM Orders o " +
           "WHERE o.user.userId = :userId " +
           "ORDER BY o.createdAt DESC")
    Page<Long> findOrderIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Orders o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product p " +
           "LEFT JOIN FETCH oi.productOption po " +
           "WHERE o.id IN :orderIds " +
           "ORDER BY o.createdAt DESC")
    List<Orders> findByIdsWithAllData(@Param("orderIds") List<Long> orderIds);

    // 🚀 성능 최적화: 사용자별 주문 조회 (Fetch Join)
    @Query("SELECT DISTINCT o FROM Orders o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product p " +
           "WHERE o.user.userId = :userId " +
           "ORDER BY o.createdAt DESC")
    List<Orders> findByUserIdWithItems(@Param("userId") Long userId);

    // 🚀 성능 최적화: 날짜 범위 조건 포함
    @Query("SELECT o.id FROM Orders o " +
           "WHERE o.user.userId = :userId " +
           "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
           "ORDER BY o.createdAt DESC")
    Page<Long> findOrderIdsByUserIdAndDate(@Param("userId") Long userId, 
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          Pageable pageable);

    // 🚀 성능 최적화: 경량 주문 요약 정보 조회 (DTO Projection)
    @Query("SELECT new com.Soo_Shinsa.order.dto.OrderSummaryDto(" +
           "o.id, " +
           "o.orderId, " +
           "o.totalPrice, " +
           "o.status, " +
           "o.createdAt, " +
           "CAST(COUNT(oi.id) AS long), " +
           "CAST(SUM(CASE WHEN oi.status = 'ORDERED' THEN 1 ELSE 0 END) AS long), " +
           "CAST(SUM(CASE WHEN oi.status IN ('CANCELLED', 'REFUNDED') THEN 1 ELSE 0 END) AS long), " +
           "COALESCE(SUM(CASE WHEN oi.status = 'ORDERED' THEN COALESCE(oi.discountPrice, oi.price) * oi.quantity ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN oi.status IN ('CANCELLED', 'REFUNDED') THEN COALESCE(oi.discountPrice, oi.price) * oi.quantity ELSE 0 END), 0), " +
           "CASE " +
           "  WHEN SUM(CASE WHEN oi.status = 'ORDERED' THEN 1 ELSE 0 END) = 0 THEN 'FULLY_CANCELLED' " +
           "  WHEN SUM(CASE WHEN oi.status IN ('CANCELLED', 'REFUNDED') THEN 1 ELSE 0 END) > 0 THEN 'PARTIALLY_CANCELLED' " +
           "  ELSE 'ACTIVE' " +
           "END, " +
           "(SELECT p.productName FROM OrderItem oi2 JOIN oi2.product p WHERE oi2.order.id = o.id ORDER BY oi2.id ASC LIMIT 1)" +
           ") " +
           "FROM Orders o " +
           "LEFT JOIN o.orderItems oi " +
           "WHERE o.user.userId = :userId " +
           "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
           "GROUP BY o.id, o.orderId, o.totalPrice, o.status, o.createdAt " +
           "ORDER BY o.createdAt DESC")
    Page<OrderSummaryDto> findOrderSummariesByUserIdAndDate(@Param("userId") Long userId,
                                                           @Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate,
                                                           Pageable pageable);

    // 🚀 성능 최적화: 간단한 주문 요약 정보 조회
    @Query("SELECT new com.Soo_Shinsa.order.dto.OrderSummaryDto(" +
           "o.id, " +
           "o.orderId, " +
           "o.totalPrice, " +
           "o.status, " +
           "o.createdAt, " +
           "CAST(COUNT(oi.id) AS long), " +
           "CAST(SUM(CASE WHEN oi.status = 'ORDERED' THEN 1 ELSE 0 END) AS long), " +
           "CAST(SUM(CASE WHEN oi.status IN ('CANCELLED', 'REFUNDED') THEN 1 ELSE 0 END) AS long), " +
           "COALESCE(SUM(CASE WHEN oi.status = 'ORDERED' THEN COALESCE(oi.discountPrice, oi.price) * oi.quantity ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN oi.status IN ('CANCELLED', 'REFUNDED') THEN COALESCE(oi.discountPrice, oi.price) * oi.quantity ELSE 0 END), 0), " +
           "CASE " +
           "  WHEN SUM(CASE WHEN oi.status = 'ORDERED' THEN 1 ELSE 0 END) = 0 THEN 'FULLY_CANCELLED' " +
           "  WHEN SUM(CASE WHEN oi.status IN ('CANCELLED', 'REFUNDED') THEN 1 ELSE 0 END) > 0 THEN 'PARTIALLY_CANCELLED' " +
           "  ELSE 'ACTIVE' " +
           "END, " +
           "(SELECT p.productName FROM OrderItem oi2 JOIN oi2.product p WHERE oi2.order.id = o.id ORDER BY oi2.id ASC LIMIT 1)" +
           ") " +
           "FROM Orders o " +
           "LEFT JOIN o.orderItems oi " +
           "WHERE o.user.userId = :userId " +
           "GROUP BY o.id, o.orderId, o.totalPrice, o.status, o.createdAt " +
           "ORDER BY o.createdAt DESC")
    Page<OrderSummaryDto> findOrderSummariesByUserId(@Param("userId") Long userId, Pageable pageable);
}
