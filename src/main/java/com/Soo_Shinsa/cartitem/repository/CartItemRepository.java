package com.Soo_Shinsa.cartitem.repository;

import com.Soo_Shinsa.cartitem.model.CartItem;
import com.Soo_Shinsa.global.exception.NotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import static com.Soo_Shinsa.global.exception.ErrorCode.NOT_FOUND_CART;

public interface CartItemRepository extends JpaRepository<CartItem, Long>, CartItemCustomRepository {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CartItem c WHERE c.user.userId = :userId")
    List<CartItem> findByUserUserIdWithLock(@Param("userId") Long userId);

    default CartItem findByIdOrElseThrow(Long cartId) {
        return findById(cartId).orElseThrow(() -> new NotFoundException(NOT_FOUND_CART));
    }
}