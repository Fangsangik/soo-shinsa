package com.Soo_Shinsa.coupon.repository;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponBrandRelation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponBrandRelationRepository extends JpaRepository<CouponBrandRelation, Long> {
    boolean existsByCouponAndBrand(Coupon coupon, Brand brand);

    CouponBrandRelation findByCoupon(Coupon coupon);
}
