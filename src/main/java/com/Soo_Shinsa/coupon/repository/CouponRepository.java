package com.Soo_Shinsa.coupon.repository;

import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    default Coupon findByIdOrElseThrow(Long couponId) {
        return findById(couponId).orElseThrow(
                () -> new NotFoundException(ErrorCode.NOT_FOUND_COUPON_COUNT)
        );
    }
}
