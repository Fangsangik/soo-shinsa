package com.Soo_Shinsa.coupon.calculate;

import com.Soo_Shinsa.constant.CouponType;
import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.exception.InvalidInputException;
import com.Soo_Shinsa.product.model.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static com.Soo_Shinsa.support.Fixtures.*;
import static org.assertj.core.api.Assertions.*;

class PercentageDiscountCalculatorTest {
    final PercentageDiscountCalculator calc = new PercentageDiscountCalculator();

    @ParameterizedTest(name = "{0}원 × {1}% 할인 = {2}원")
    @CsvSource({"10000, 10, 9000", "19900, 10, 17910", "10000, 0, 10000", "10000, 100, 0", "33333, 33, 22333.11"})
    void 할인율은_퍼센트_단위다(String price, String rate, String expected) {
        assertThat(calc.calculateDiscountedPrice(new BigDecimal(price), new BigDecimal(rate))).isEqualByComparingTo(expected);
    }

    @Test
    void 범위를_벗어난_할인율은_거부() {
        assertThatThrownBy(() -> calc.calculateDiscountedPrice(BigDecimal.TEN, new BigDecimal("101"))).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> calc.calculateDiscountedPrice(BigDecimal.TEN, new BigDecimal("-1"))).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void 특정_상품_쿠폰은_그_상품에만() {
        Product target = product(1L, brand(1L, user(1L, Role.VENDOR)), 10_000);
        Product another = product(2L, target.getBrand(), 10_000);
        Coupon c = coupon(1L, 50, CouponType.SPECIFIC_PRODUCT, 10);
        ReflectionTestUtils.setField(c, "product", target);

        assertThat(calc.applyCoupon(c, target, target.getPrice())).isEqualByComparingTo("5000");
        assertThatThrownBy(() -> calc.applyCoupon(c, another, another.getPrice())).isInstanceOf(InvalidInputException.class);
    }
}
