package com.Soo_Shinsa.coupon.repository;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponBrandRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponBrandRelationRepository extends JpaRepository<CouponBrandRelation, Long> {
    boolean existsByCouponAndBrand(Coupon coupon, Brand brand);

    CouponBrandRelation findByCoupon(Coupon coupon);

    /** 발급 시 브랜드별 잔여 수량을 깎기 위해 쿠폰에 걸린 브랜드들을 가져온다. */
    List<CouponBrandRelation> findAllByCouponId(Long couponId);
}
