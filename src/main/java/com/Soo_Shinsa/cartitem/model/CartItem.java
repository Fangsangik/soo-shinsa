package com.Soo_Shinsa.cartitem.model;

import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.global.constant.BaseTimeEntity;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import com.Soo_Shinsa.user.model.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "cartitems")
public class CartItem extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productoption_Id", nullable = false)
    private ProductOption productOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    private BigDecimal discountedPrice;


    @Builder
    public CartItem(Integer quantity, User user, ProductOption productOption, Product product, Coupon coupon, BigDecimal discountedPrice) {
        this.quantity = quantity;
        this.user = user;
        this.productOption = productOption;
        this.product = product;
        this.coupon = coupon;
        this.discountedPrice = discountedPrice;
    }

    public void updateCartItem(Integer quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }

        this.quantity = quantity;
    }

    public void applyCoupon(Coupon coupon, BigDecimal discountedPrice) {
        this.coupon = coupon; // 쿠폰 정보 저장
        this.discountedPrice = discountedPrice; // 할인된 가격 저장
    }

    public void applyCoupon(Coupon coupon) {
        this.coupon = coupon; // 쿠폰 정보 저장
    }
}