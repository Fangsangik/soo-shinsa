package com.Soo_Shinsa.coupon.model;

import com.Soo_Shinsa.user.model.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "coupon_user", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"coupon_id", "user_id"})
})
public class CouponUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private boolean isUsed = false;

    private LocalDate usedAt;

    @Builder
    public CouponUser(Coupon coupon, User user, boolean isUsed, LocalDate usedAt) {
        this.coupon = coupon;
        this.user = user;
        this.isUsed = isUsed;
        this.usedAt = usedAt;
    }

    public void markAsUsed() {
        if (this.isUsed) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }
        this.isUsed = true;
        this.usedAt = LocalDate.now();
    }
}