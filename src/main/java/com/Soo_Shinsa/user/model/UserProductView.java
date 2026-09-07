package com.Soo_Shinsa.user.model;

import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class UserProductView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne
    @JoinColumn(name = "product_option_id")
    private ProductOption productOption;

    /** 상품 상세를 본 기록. 옵션을 특정할 수 없는 경로에서 쓴다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private LocalDate viewDate;

    public UserProductView(User user, ProductOption productOption, LocalDate viewDate) {
        this.user = user;
        this.productOption = productOption;
        this.product = productOption != null ? productOption.getProduct() : null;
        this.viewDate = viewDate;
    }

    public UserProductView(User user, Product product, LocalDate viewDate) {
        this.user = user;
        this.product = product;
        this.viewDate = viewDate;
    }
}
