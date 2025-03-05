package com.Soo_Shinsa.user.model;

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

    private LocalDate viewDate;

    public UserProductView(User user, ProductOption productOption, LocalDate viewDate) {
        this.user = user;
        this.productOption = productOption;
        this.viewDate = viewDate;
    }
}
