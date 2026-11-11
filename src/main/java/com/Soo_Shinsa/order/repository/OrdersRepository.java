package com.Soo_Shinsa.order.repository;

import com.Soo_Shinsa.exception.NotFoundException;
import com.Soo_Shinsa.order.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import static com.Soo_Shinsa.exception.ErrorCode.NOT_FOUND_ORDER;

public interface OrdersRepository extends JpaRepository<Orders, Long>, OrderCustomRepository {

    default Orders findByIdOrElseThrow(Long orderId) {
        return findById(orderId).orElseThrow(
                () -> new NotFoundException(NOT_FOUND_ORDER));
    }
    Orders findByOrderId(String orderId);

    /** 입점사가 이 주문의 상품 중 하나라도 소유하는지 */
    @org.springframework.data.jpa.repository.Query("select count(oi) > 0 from OrderItem oi where oi.order.id = :orderId and oi.product.brand.user.userId = :userId")
    boolean existsByIdAndVendorUserId(@org.springframework.data.repository.query.Param("orderId") Long orderId, @org.springframework.data.repository.query.Param("userId") Long userId);
}
