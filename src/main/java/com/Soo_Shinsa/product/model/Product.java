package com.Soo_Shinsa.product.model;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.global.constant.BaseTimeEntity;
import com.Soo_Shinsa.global.constant.ProductStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private ProductStatus productStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Builder
    public Product(String name, BigDecimal price, String imageUrl, ProductStatus productStatus, Brand brand) {
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.productStatus = productStatus;
        this.brand = brand;
    }

    public void update(String name, BigDecimal price, ProductStatus productStatus, String imageUrl) {

        if (name != null) {
            this.name = name;
        }

        if (price != null) {
            this.price = price;
        }

        if (productStatus != null) {
            this.productStatus = productStatus;
        }

        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }

    }
}
