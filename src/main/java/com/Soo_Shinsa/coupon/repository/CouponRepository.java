package com.Soo_Shinsa.coupon.repository;

import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.exception.ErrorCode;
import com.Soo_Shinsa.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface CouponRepository extends JpaRepository<Coupon, Long> {

    default Coupon findByIdOrElseThrow(Long couponId) {
        return findById(couponId).orElseThrow(
                () -> new NotFoundException(ErrorCode.NOT_FOUND_COUPON_COUNT)
        );
    }

    @Modifying
    @Query("UPDATE Coupon c SET c.issuedCount = c.issuedCount + 1 WHERE c.id = :couponId AND c.issuedCount < c.maxCount")
    int incrementIssuedCount(@Param("couponId") Long couponId);
}
