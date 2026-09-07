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

import java.time.LocalDateTime;
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

    // 승인 관련 필드들
    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "approved_by")
    private Long approvedBy; // 승인한 관리자 ID

    @Column(name = "approval_reason")
    private String approvalReason;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "admin_comment")
    private String adminComment;

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

    // 승인 관련 메소드들
    public void approve(Long adminId, String approvalReason, String adminComment) {
        this.status = BrandStatus.OPEN;
        this.approvalDate = LocalDateTime.now();
        this.approvedBy = adminId;
        this.approvalReason = approvalReason;
        this.adminComment = adminComment;
    }

    public void reject(Long adminId, String rejectionReason, String adminComment) {
        this.status = BrandStatus.REJECT;
        this.approvalDate = LocalDateTime.now();
        this.approvedBy = adminId;
        this.rejectionReason = rejectionReason;
        this.adminComment = adminComment;
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

