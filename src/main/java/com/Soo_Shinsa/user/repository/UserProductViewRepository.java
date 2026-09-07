package com.Soo_Shinsa.user.repository;

import com.Soo_Shinsa.user.model.UserProductView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserProductViewRepository extends JpaRepository<UserProductView, Long> {

    @Query("SELECT upv FROM UserProductView upv WHERE upv.user.userId = :userId ORDER BY upv.viewDate DESC")
    List<UserProductView> findRecentlyViewedProductOptions(@Param("userId") Long userId, Pageable pageable);

    /** 내가 본 상품 id */
    @Query("SELECT DISTINCT upv.product.id FROM UserProductView upv " +
           "WHERE upv.user.userId = :userId AND upv.product IS NOT NULL")
    List<Long> findViewedProductIds(@Param("userId") Long userId);

    /**
     * 함께 본 상품.
     *
     * 내가 본 상품을 똑같이 본 다른 사용자들이, 그 밖에 무엇을 봤는지 세어 많이 본 순으로 돌려준다.
     * 브랜드가 같은 상품만 보여주던 기존 방식보다 넓게 추천된다.
     */
    @Query(value =
            "SELECT other.product_id " +
            "FROM user_product_view other " +
            "WHERE other.product_id IS NOT NULL " +
            "  AND other.user_user_id <> :userId " +
            "  AND other.product_id NOT IN (:viewedProductIds) " +
            "  AND other.user_user_id IN ( " +
            "      SELECT peer.user_user_id FROM user_product_view peer " +
            "      WHERE peer.product_id IN (:viewedProductIds) AND peer.user_user_id <> :userId) " +
            "GROUP BY other.product_id " +
            "ORDER BY COUNT(DISTINCT other.user_user_id) DESC, other.product_id " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Long> findCoViewedProductIds(@Param("userId") Long userId,
                                      @Param("viewedProductIds") List<Long> viewedProductIds,
                                      @Param("limit") int limit);
}
