package com.Soo_Shinsa.cartitem.model;

import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.global.constant.BaseTimeEntity;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.user.model.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class CartItem extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    private BigDecimal discountedPrice;

    // ✅ 여러 개의 옵션을 저장하는 리스트
    @OneToMany(mappedBy = "cartItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItemProductOption> productOptions = new ArrayList<>();

    @Builder
    public CartItem(Integer quantity, User user, Product product, Coupon coupon, BigDecimal discountedPrice) {
        this.quantity = quantity;
        this.user = user;
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
