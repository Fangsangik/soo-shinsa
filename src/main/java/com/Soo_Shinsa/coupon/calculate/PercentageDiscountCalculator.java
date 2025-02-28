package com.Soo_Shinsa.coupon.calculate;

import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.Soo_Shinsa.product.model.Product;

import java.math.BigDecimal;
import java.util.List;

public class PercentageDiscountCalculator implements DiscountCouponCalculator {
    @Override
    public BigDecimal calculateDiscountedPrice(BigDecimal originalPrice, BigDecimal discountRate) {
        if (discountRate.compareTo(BigDecimal.ZERO) < 0 || discountRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidInputException(ErrorCode.NOT_APPROPRIATE_PERCENTAGE);
        }

        // 할인 계산 로직 (끝까지 계산)
        return originalPrice.subtract(originalPrice.multiply(discountRate).divide(BigDecimal.valueOf(100)));
    }

    @Override
    public BigDecimal applyCoupon(Coupon coupon, Product product, BigDecimal originalPrice) {
        switch (coupon.getCouponType()) {
            case SPECIFIC_PRODUCT:
                if (coupon.getProduct() == null || !coupon.getProduct().getId().equals(product.getId())) {
                    throw new InvalidInputException(ErrorCode.NOT_APPROPRIATE_COUPON);
                }
                break;

            case SPECIFIC_BRAND:
                // 특정 브랜드용 쿠폰 확인
                List<Long> couponBrandIds = coupon.getCouponBrandRelations().stream()
                        .map(relation -> relation.getBrand().getId())
                        .toList();

                if (!couponBrandIds.contains(product.getBrand().getId())) {
                    throw new InvalidInputException(ErrorCode.NOT_APPROPRIATE_COUPON);
                }
                break;

            case UNIVERSAL:
                // 모든 상품에 적용 가능
                break;

            default:
                throw new IllegalStateException("알 수 없는 쿠폰 타입입니다.");
        }

        // 할인 계산
        return calculateDiscountedPrice(originalPrice, coupon.getDiscountRate());
    }
}
