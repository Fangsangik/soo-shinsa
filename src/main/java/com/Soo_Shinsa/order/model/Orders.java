package com.Soo_Shinsa.order.model;


import com.Soo_Shinsa.global.constant.BaseTimeEntity;
import com.Soo_Shinsa.global.constant.OrdersStatus;
import com.Soo_Shinsa.user.model.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "orders")
public class Orders extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrdersStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    private BigDecimal discountPrice;

    @Builder
    public Orders(BigDecimal totalPrice, OrdersStatus status, User user, List<OrderItem> orderItems, BigDecimal discountPrice) {
        this.orderId = createOrderNumber();
        this.totalPrice = totalPrice;
        this.status = status;
        this.user = user;
        this.orderItems = new ArrayList<>(); // Null 체크 후 초기화
        this.discountPrice = discountPrice;
    }

    @Builder
    public Orders(BigDecimal totalPrice, OrdersStatus status, User user) {
        this.orderId = createOrderNumber();
        this.totalPrice = totalPrice;
        this.status = status;
        this.user = user;
        this.orderItems = new ArrayList<>(); // 명시적 초기화
    }

    @Builder
    public Orders(OrdersStatus status, User user) {
        this.orderId = createOrderNumber();
        this.status = status;
        this.user = user;
        this.orderItems = new ArrayList<>(); // 명시적 초기화
    }


    // 연관관계 오더 아이템 추가
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        calculateTotalPrice();
        // 생성자에서 이미 order 설정 완료, 별도의 set 호출 필요 없음
    }

    //연관관계 오더 아이템 제거
    public void removeOrderItem(OrderItem orderItem) {
        this.orderItems.remove(orderItem);
        calculateTotalPrice();
        // 삭제 시에도 연관 관계를 직접 null로 설정하지 않아도 됨

    }

    // 총 결제 금액 계산
    public void calculateTotalPrice() {
        this.totalPrice = orderItems.stream()
                .map(item -> {
                    BigDecimal effectivePrice = (item.getDiscountPrice() != null && item.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0)
                            ? item.getDiscountPrice()
                            : item.getProduct().getPrice();
                    return effectivePrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String createOrderNumber() {
        return orderId = "ORD-" + UUID.randomUUID();
    }

    public void updateStatus(OrdersStatus status) {
        this.status = status;
    }

    public void updateTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
