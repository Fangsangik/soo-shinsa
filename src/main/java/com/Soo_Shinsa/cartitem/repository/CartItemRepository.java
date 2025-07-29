package com.Soo_Shinsa.cartitem.repository;

import com.Soo_Shinsa.cartitem.model.CartItem;
import com.Soo_Shinsa.global.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import static com.Soo_Shinsa.global.exception.ErrorCode.NOT_FOUND_CART;

public interface CartItemRepository extends JpaRepository<CartItem, Long>, CartItemCustomRepository {

    @Query("SELECT DISTINCT c FROM CartItem c " +
           "LEFT JOIN FETCH c.product p " +
           "LEFT JOIN FETCH p.brand b " +
           "LEFT JOIN FETCH c.productOptions cpo " +
           "LEFT JOIN FETCH cpo.productOption po " +
           "LEFT JOIN FETCH c.coupon cp " +
           "WHERE c.user.userId = :userId")
    List<CartItem> findByUserUserIdWithLock(@Param("userId") Long userId);

    @Query("SELECT DISTINCT c FROM CartItem c " +
           "LEFT JOIN FETCH c.product p " +
           "LEFT JOIN FETCH p.brand b " +
           "LEFT JOIN FETCH c.productOptions cpo " +
           "LEFT JOIN FETCH cpo.productOption po " +
           "LEFT JOIN FETCH c.coupon cp " +
           "WHERE c.id = :cartId")
    CartItem findByIdWithFetch(@Param("cartId") Long cartId);

    default CartItem findByIdOrElseThrow(Long cartId) {
        CartItem cartItem = findByIdWithFetch(cartId);
        if (cartItem == null) {
            throw new NotFoundException(NOT_FOUND_CART);
        }
        return cartItem;
    }
}