package com.Soo_Shinsa.product.repository;

import com.Soo_Shinsa.exception.NotFoundException;
import com.Soo_Shinsa.product.model.ProductOption;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import static com.Soo_Shinsa.exception.ErrorCode.NOT_FOUND_PRODUCT_OPTION;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long>, ProductOptionCustomRepository {
    List<ProductOption> findProductOptionByProductId(Long productId);

    void deleteAllByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductOption p WHERE p.id = :id")
    Optional<ProductOption> findByIdWithLock(@Param("id") Long id);


    default ProductOption findByIdOrElseThrow(Long productOptionId) {
        return findById(productOptionId).orElseThrow(() -> new NotFoundException(NOT_FOUND_PRODUCT_OPTION));
    }
}