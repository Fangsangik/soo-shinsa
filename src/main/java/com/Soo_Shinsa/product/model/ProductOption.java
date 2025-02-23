package com.Soo_Shinsa.product.model;

import com.Soo_Shinsa.constant.BaseTimeEntity;
import com.Soo_Shinsa.constant.ProductStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String size;

    private String color;

    @Enumerated(EnumType.STRING)
    private ProductStatus productStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private Integer quantity;
    @Builder
    public ProductOption(String size, String color, ProductStatus productStatus, Product product, Integer quantity) {
        this.size = size;
        this.color = color;
        this.productStatus = productStatus;
        this.product = product;
        this.quantity = quantity;
    }

    public void update(String size, String color, ProductStatus productStatus, int quantity) {

        if (size != null) {
            this.size = size;
        }

        if (color != null) {
            this.color = color;
        }

        if (productStatus != null) {
            this.productStatus = productStatus;
        }

        if (quantity != 0) {
            this.quantity = quantity;
        }
    }

    public void updateQuantity(int change) {
        if (this.quantity + change < 0) {
            throw new IllegalArgumentException("재고 수량이 부족합니다.");
        }
        this.quantity += change;
    }

    public void decreaseQuantity(int quantity) {
        if (this.quantity < quantity) {
            throw new IllegalArgumentException("재고 수량이 부족합니다.");
        }
        this.quantity -= quantity;
    }
}
