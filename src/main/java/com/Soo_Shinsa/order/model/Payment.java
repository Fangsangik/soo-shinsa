package com.Soo_Shinsa.order.model;

import com.Soo_Shinsa.global.constant.BaseTimeEntity;
import com.Soo_Shinsa.global.constant.TossPayMethod;
import com.Soo_Shinsa.global.constant.TossPayStatus;
import com.Soo_Shinsa.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "payments")
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String paymentKey; // 토스페이먼츠에서 제공하는 고유 결제 키

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private BigDecimal amount; // 결제 금액

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TossPayStatus status; // 결제 상태 (READY, DONE, CANCELLED 등)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TossPayMethod method; // 결제 수단 (CARD)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_id", nullable = false)
    private Orders order; // Orders 엔티티와의 1:1 연관 관계

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user; // User 엔티티와의 다대일 연관 관계

    public Payment(String paymentKey, String orderId, BigDecimal amount, TossPayStatus status, TossPayMethod method, Orders order, User user) {
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.method = method;
        this.order = order;
        this.user = user;
    }

    public Payment(String orderId, BigDecimal amount, TossPayStatus status, TossPayMethod method, Orders order, User user) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.method = method;
        this.order = order;
        this.user = user;
    }

    public void update(TossPayStatus status, String paymentKey) {
        this.status = status;
        this.paymentKey = paymentKey;
    }

}



