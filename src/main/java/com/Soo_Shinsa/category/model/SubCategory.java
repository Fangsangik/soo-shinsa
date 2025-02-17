package com.Soo_Shinsa.category.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class SubCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Builder
    public SubCategory(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    public void update(String name) {
        if (name != null) {
            this.name = name;
        }
    }
}
