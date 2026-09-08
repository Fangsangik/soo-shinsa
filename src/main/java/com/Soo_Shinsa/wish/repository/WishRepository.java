package com.Soo_Shinsa.wish.repository;

import com.Soo_Shinsa.wish.model.Wish;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    boolean existsByUserUserIdAndProductId(Long userId, Long productId);

    Optional<Wish> findByUserUserIdAndProductId(Long userId, Long productId);

    @EntityGraph(attributePaths = {"product", "product.brand"})
    List<Wish> findAllByUserUserIdOrderByIdDesc(Long userId);
}
