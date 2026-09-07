package com.Soo_Shinsa.product.repository;

import com.Soo_Shinsa.global.exception.NotFoundException;
import com.Soo_Shinsa.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import static com.Soo_Shinsa.global.exception.ErrorCode.NOT_FOUND_PRODUCT;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductCustomRepository {

    default Product findByIdOrElseThrow(Long productId) {
        return findById(productId).orElseThrow(() -> new NotFoundException(NOT_FOUND_PRODUCT));
    }

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.brand b " +
           "LEFT JOIN FETCH b.subCategory sc " +
           "WHERE p.brand.id = :brandId AND p.id <> :productId")
    Page<Product> findByBrandAndProductId(@Param("brandId") Long brandId,
                                          @Param("productId") Long productId,
                                          Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.brand b " +
           "LEFT JOIN FETCH b.subCategory sc " +
           "LEFT JOIN ProductOption po ON po.product = p " +
           "GROUP BY p.id ORDER BY SUM(po.salesCount) DESC")
    Page<Product> findBestSellingProducts(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.brand b " +
           "LEFT JOIN FETCH b.subCategory sc " +
           "ORDER BY FUNCTION('RAND')")
    Page<Product> findRandomProducts(Pageable pageable);

    /** 자동완성: 상품명에 키워드가 포함된 이름을 중복 없이. 앞에서 일치하는 것을 먼저 보여준다. */
    @Query("SELECT DISTINCT p.name FROM Product p " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY LOCATE(LOWER(:keyword), LOWER(p.name)), p.name")
    List<String> findNameSuggestions(@Param("keyword") String keyword, Pageable pageable);
}
