package com.Soo_Shinsa.cartitem.model;

import com.Soo_Shinsa.product.model.ProductOption;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "cartitem_product_options")
public class CartItemProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartitem_id", nullable = false)
    private CartItem cartItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productoption_id", nullable = false)
    private ProductOption productOption;

    public CartItemProductOption(CartItem cartItem, ProductOption productOption) {
        this.cartItem = cartItem;
        this.productOption = productOption;
    }
}

