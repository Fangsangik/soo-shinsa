package com.Soo_Shinsa.support;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.cartitem.model.CartItem;
import com.Soo_Shinsa.constant.*;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponUser;
import com.Soo_Shinsa.order.model.OrderItem;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import com.Soo_Shinsa.user.model.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

/** 단위 테스트용 엔티티 빌더. 엔티티에 setter 가 없으므로 id 는 리플렉션으로 넣는다. */
public final class Fixtures {
    private Fixtures() {}

    public static <T> T withId(T entity, String field, Long id) {
        ReflectionTestUtils.setField(entity, field, id);
        return entity;
    }

    public static User user(Long id, Role role) {
        User u = User.builder().email(role.name().toLowerCase() + id + "@test.com").password("pw").name(role.name() + id)
                .phoneNum("010-0000-0000").status(UserStatus.ACTIVE).role(role).build();
        return withId(u, "userId", id);
    }

    public static Brand brand(Long id, User owner) {
        Brand b = Brand.builder().registrationNum("000-00-" + id).name("brand" + id).context("ctx").status(BrandStatus.OPEN)
                .user(owner).couponCount(0).isCouponLimited(false).build();
        return withId(b, "id", id);
    }

    public static Product product(Long id, Brand brand, long price) {
        Product p = Product.builder().name("product" + id).price(BigDecimal.valueOf(price)).productStatus(ProductStatus.AVAILABLE).brand(brand).build();
        return withId(p, "id", id);
    }

    public static ProductOption option(Long id, Product product) {
        ProductOption o = ProductOption.builder().size("M").color("Black").productStatus(ProductStatus.AVAILABLE).product(product).build();
        return withId(o, "id", id);
    }

    public static CartItem cartItem(Long id, User user, Product product, ProductOption option, int qty) {
        CartItem c = CartItem.builder().quantity(qty).user(user).product(product).productOption(option).build();
        return withId(c, "id", id);
    }

    public static Orders order(Long id, User user, OrdersStatus status) {
        Orders o = Orders.builder().user(user).status(status).totalPrice(BigDecimal.ZERO).build();
        return withId(o, "id", id);
    }

    public static OrderItem orderItem(Long id, Orders order, Product product, int qty, BigDecimal discountPrice) {
        OrderItem oi = OrderItem.builder().order(order).product(product).quantity(qty).price(product.getPrice()).discountPrice(discountPrice).build();
        return withId(oi, "id", id);
    }

    public static Coupon coupon(Long id, int percent, CouponType type, int maxCount) {
        Coupon c = Coupon.builder().couponName("c" + id).discountRate(BigDecimal.valueOf(percent)).couponType(type).maxCount(maxCount).build();
        return withId(c, "id", id);
    }

    public static CouponUser couponUser(Long id, Coupon coupon, User user, boolean used) {
        CouponUser cu = CouponUser.builder().coupon(coupon).user(user).isUsed(used).build();
        return withId(cu, "id", id);
    }
}
