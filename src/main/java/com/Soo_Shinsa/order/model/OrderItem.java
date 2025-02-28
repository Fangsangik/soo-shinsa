package com.Soo_Shinsa.order.model;

import com.Soo_Shinsa.global.constant.BaseTimeEntity;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "orderItems")
public class OrderItem extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_id", nullable = false)
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", nullable = false)
    private ProductOption productOption;

    private BigDecimal price;

    private BigDecimal discountPrice;

    @Builder
    public OrderItem(Integer quantity, Orders order, Product product, ProductOption productOption, BigDecimal price, BigDecimal discountPrice) {
        this.quantity = quantity;
        this.order = order;
        this.product = product;
        this.productOption = productOption;
        this.price = price;
        this.discountPrice = discountPrice;
    }

    public void updateOrderItem(Integer quantity) {
        this.quantity = quantity;
    }


}
