package com.Soo_Shinsa.coupon.model;

import com.Soo_Shinsa.constant.CouponType;
import com.Soo_Shinsa.exception.ErrorCode;
import com.Soo_Shinsa.exception.InternalServerException;
import com.Soo_Shinsa.product.model.Product;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String couponName;
    private String couponCode;

    private BigDecimal discountRate;

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CouponBrandRelation> couponBrandRelations = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    private CouponType couponType;

    private boolean isUsed;

    private Integer maxCount; // 쿠폰 발급 최대 수량
    private Integer issuedCount; // 쿠폰 발급 수량

    private LocalDate expirationDate;
    private LocalDate issueDate;

    @Builder
    public Coupon(String couponName, BigDecimal discountRate, Product product, boolean isUsed, CouponType couponType, Integer maxCount) {
        this.couponName = couponName;
        this.couponCode = createCouponNumber();
        this.discountRate = discountRate;
        this.product = product;
        this.isUsed = isUsed;
        this.couponType = couponType;
        this.expirationDate = LocalDate.now().plusDays(7);
        this.issueDate = LocalDate.now();
        this.maxCount = maxCount;
        this.issuedCount = 0;
    }

    public String createCouponNumber() {
       return this.couponCode = "COUPON" + UUID.randomUUID().toString().substring(0, 8);
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expirationDate);
    }

    // 재고 감소 메서드
    public void decreaseMaxCount(int amount) {
        if (this.maxCount != null && this.maxCount >= amount) {
            this.maxCount -= amount;
        } else {
            throw new InternalServerException(ErrorCode.COUPON_OUT_OF_STOCK);
        }
    }

    public void increaseIssuedCount() {
        if (this.issuedCount >= this.maxCount) {
            throw new IllegalStateException("쿠폰 발급 한도를 초과했습니다.");
        }
        this.issuedCount++;
    }
}
