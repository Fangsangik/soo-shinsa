package com.Soo_Shinsa.wish.model;

import com.Soo_Shinsa.global.constant.BaseCreatedTimeEntity;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.user.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 찜. 같은 상품 중복 찜은 유니크 제약이 막는다. */
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "wish", uniqueConstraints = @UniqueConstraint(name = "uk_wish_user_product", columnNames = {"user_id", "product_id"}))
public class Wish extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    public Wish(User user, Product product) {
        this.user = user;
        this.product = product;
    }
}
