package com.Soo_Shinsa.category.repository;

import com.Soo_Shinsa.cartitem.model.CartItemProductOption;
import com.Soo_Shinsa.product.model.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemProductOptionRepository extends JpaRepository<CartItemProductOption, Long> {
    @Modifying
    @Query("DELETE FROM CartItemProductOption cpo WHERE cpo.cartItem.id = :cartItemId")
    void deleteByCartItemId(@Param("cartItemId") Long cartItemId);

    @Modifying
    @Query("DELETE FROM CartItemProductOption cpo WHERE cpo.cartItem.id IN :cartItemIds")
    void deleteByCartItemIds(@Param("cartItemIds") List<Long> cartItemIds);

    @Query("SELECT cpo.productOption FROM CartItemProductOption cpo WHERE cpo.cartItem.id = :cartItemId")
    List<ProductOption> findProductOptionsByCartItemId(@Param("cartItemId") Long cartItemId);

    boolean existsByCartItemId(Long id);
}
