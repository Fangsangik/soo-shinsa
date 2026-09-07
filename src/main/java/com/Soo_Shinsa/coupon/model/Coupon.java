package com.Soo_Shinsa.coupon.model;

import com.Soo_Shinsa.global.constant.CouponType;
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

    private Integer maxCount; // 발급 정원. 만들어진 뒤 바뀌지 않는다
    private Integer issuedCount; // 지금까지 발급된 수
    private Integer remainingCount; // 아직 쓸 수 있는 수

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
        this.remainingCount = maxCount;
    }

    /**
     * 아직 쓸 수 있는 수.
     * remainingCount 도입 전에 만들어진 쿠폰은 값이 없으므로 정원으로 간주한다.
     */
    public Integer getRemainingCount() {
        return remainingCount != null ? remainingCount : maxCount;
    }

    public String createCouponNumber() {
       return this.couponCode = "COUPON" + UUID.randomUUID().toString().substring(0, 8);
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expirationDate);
    }

}
