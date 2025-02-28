package com.Soo_Shinsa.order.repository;

import com.Soo_Shinsa.global.exception.NotFoundException;
import com.Soo_Shinsa.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import static com.Soo_Shinsa.global.exception.ErrorCode.NOT_FOUND_ORDER_OPTION;


public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, OrderItemCustomRepository {

    default OrderItem findByIdOrElseThrow(Long orderItemId) {
        return findById(orderItemId).orElseThrow(
                () -> new NotFoundException(NOT_FOUND_ORDER_OPTION));
    }

    void deleteAllByProductId(Long productId);
}
