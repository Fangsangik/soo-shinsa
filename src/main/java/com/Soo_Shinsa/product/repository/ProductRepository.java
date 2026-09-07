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

    /**
     * 자동완성: 상품명에 키워드가 포함된 이름을 중복 없이. 앞에서 일치하는 것을 먼저 보여준다.
     *
     * LIKE '%키워드%' 는 인덱스를 타지 못해 매번 전체 스캔이었다.
     * ngram FULLTEXT 인덱스를 쓰면 희소 키워드에서 20배 이상 빨라진다.
     *
     * @param phrase BOOLEAN MODE 구문 (따옴표로 감싼 형태). 연산자 오입력을 막기 위해 서비스에서 만든다.
     * @param raw    정렬용 원본 키워드
     */
    @Query(value = "SELECT DISTINCT p.name FROM product p " +
                   "WHERE MATCH(p.name) AGAINST (:phrase IN BOOLEAN MODE) " +
                   "ORDER BY LOCATE(:raw, p.name), p.name LIMIT :limit",
           nativeQuery = true)
    List<String> findNameSuggestions(@Param("phrase") String phrase,
                                     @Param("raw") String raw,
                                     @Param("limit") int limit);
}
