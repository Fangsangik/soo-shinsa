package com.Soo_Shinsa.coupon.repository;

import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponUser;
import com.Soo_Shinsa.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponUserRepository extends JpaRepository<CouponUser, Long> {

    Optional<CouponUser> findByCouponIdAndUserUserId(Long couponId, Long userId);

    // 모든 사용자에 대해 특정 쿠폰을 조회 (isUsed가 false인 항목만 반환)
    @Query("SELECT cu FROM CouponUser cu WHERE cu.coupon.id = :couponId AND cu.isUsed = false")
    Optional<CouponUser> findUnusedCouponByCouponId(@Param("couponId") Long couponId);


    boolean existsByCouponAndUser(Coupon coupon, User user);

    @Query("SELECT cu FROM CouponUser cu JOIN FETCH cu.coupon WHERE cu.user.userId = :userId ORDER BY cu.id DESC")
    java.util.List<CouponUser> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT cu.coupon.id FROM CouponUser cu WHERE cu.user.userId = :userId AND cu.isUsed = true")
    java.util.List<Long> findUsedCouponIdsByUserId(@Param("userId") Long userId);
}
