package com.Soo_Shinsa.product.repository;

import com.Soo_Shinsa.global.exception.NotFoundException;
import com.Soo_Shinsa.product.model.ProductOption;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import static com.Soo_Shinsa.global.exception.ErrorCode.NOT_FOUND_PRODUCT_OPTION;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long>, ProductOptionCustomRepository {
    List<ProductOption> findProductOptionByProductId(Long productId);

    void deleteAllByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductOption p WHERE p.id = :id")
    Optional<ProductOption> findByIdWithLock(@Param("id") Long id);


    default ProductOption findByIdOrElseThrow(Long productOptionId) {
        return findById(productOptionId).orElseThrow(() -> new NotFoundException(NOT_FOUND_PRODUCT_OPTION));
    }

    /**
     * 재고 차감 + 판매수량 증가를 한 문장으로 처리한다.
     * 벌크 UPDATE 후 영속성 컨텍스트에 남은 엔티티를 다시 save 하면 차감이 되돌아가므로
     * clearAutomatically 로 컨텍스트를 비우고, salesCount 도 같은 문장에서 올린다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductOption p SET p.quantity = p.quantity - :quantity, p.salesCount = p.salesCount + :quantity " +
           "WHERE p.id = :productOptionId AND p.quantity >= :quantity")
    int decreaseStock(Long productOptionId, Integer quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductOption p SET p.quantity = p.quantity + :quantity, " +
           "p.salesCount = CASE WHEN p.salesCount >= :quantity THEN p.salesCount - :quantity ELSE 0 END " +
           "WHERE p.id = :productOptionId")
    int increaseStock(Long productOptionId, Integer quantity);
}