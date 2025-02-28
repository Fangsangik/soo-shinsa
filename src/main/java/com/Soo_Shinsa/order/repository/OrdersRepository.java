package com.Soo_Shinsa.order.repository;

import com.Soo_Shinsa.global.exception.NotFoundException;
import com.Soo_Shinsa.order.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import static com.Soo_Shinsa.global.exception.ErrorCode.NOT_FOUND_ORDER;

public interface OrdersRepository extends JpaRepository<Orders, Long>, OrderCustomRepository {

    default Orders findByIdOrElseThrow(Long orderId) {
        return findById(orderId).orElseThrow(
                () -> new NotFoundException(NOT_FOUND_ORDER));
    }
    Orders findByOrderId(String orderId);
}
