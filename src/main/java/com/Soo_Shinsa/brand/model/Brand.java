package com.Soo_Shinsa.brand.model;

import com.Soo_Shinsa.category.model.SubCategory;
import com.Soo_Shinsa.coupon.model.CouponBrandRelation;
import com.Soo_Shinsa.global.constant.BaseTimeEntity;
import com.Soo_Shinsa.global.constant.BrandStatus;
import com.Soo_Shinsa.user.model.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Brand extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String registrationNum;

    private String name;

    private String context;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id", nullable = false)
    private SubCategory subCategory;

    @Enumerated(EnumType.STRING)
    private BrandStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CouponBrandRelation> couponBrandRelations = new ArrayList<>();

    private Integer couponCount;
    private Boolean isCouponLimited = false;

    @Builder
    public Brand(String registrationNum, String name, String context, SubCategory subCategory, BrandStatus status, User user, List<CouponBrandRelation> couponBrandRelations, Integer couponCount, Boolean isCouponLimited) {
        this.registrationNum = registrationNum;
        this.name = name;
        this.context = context;
        this.subCategory = subCategory;
        this.status = status;
        this.user = user;
        this.couponBrandRelations = couponBrandRelations;
        this.couponCount = couponCount;
        this.isCouponLimited = isCouponLimited;
    }

    public void update(String registrationNum, String name, String context, BrandStatus status) {
        this.registrationNum = registrationNum;
        this.name = name;
        this.context = context;
        this.status = status;
    }

    // Brand.java
    public void decreaseCouponCount() {
        if (Boolean.TRUE.equals(this.isCouponLimited)) {
            if (this.couponCount == null || this.couponCount <= 0) {
                throw new IllegalStateException("발급 가능한 쿠폰이 부족합니다.");
            }
            this.couponCount--;
        }
    }
}

