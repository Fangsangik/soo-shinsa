package com.Soo_Shinsa.product.repository;

import com.Soo_Shinsa.global.constant.ProductStatus;
import com.Soo_Shinsa.product.dto.FindProductRequestDto;
import com.querydsl.core.BooleanBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 검색 조건 조립 검증. brandId가 null이면 브랜드 제한이 붙지 않아야 통합 검색이 된다.
 */
class ProductSearchPredicateTest {

    @Test
    void brandId가_없으면_브랜드_조건이_붙지_않는다() {
        BooleanBuilder b = ProductCustomRepositoryImpl.searchPredicate(
                null, new FindProductRequestDto("셔츠", null, null, null, null));
        assertFalse(b.toString().contains("brand.id"), b.toString());
        assertTrue(b.toString().contains("셔츠"), b.toString());
    }

    @Test
    void brandId가_있으면_브랜드로_제한한다() {
        BooleanBuilder b = ProductCustomRepositoryImpl.searchPredicate(
                7L, new FindProductRequestDto(null, null, null, null, null));
        assertTrue(b.toString().contains("brand.id"), b.toString());
    }

    @Test
    void 빈_키워드는_조건에서_제외된다() {
        BooleanBuilder empty = ProductCustomRepositoryImpl.searchPredicate(
                null, new FindProductRequestDto("   ", null, null, null, null));
        assertFalse(empty.hasValue());
    }

    @Test
    void 가격_상태_카테고리_조건이_모두_반영된다() {
        BooleanBuilder b = ProductCustomRepositoryImpl.searchPredicate(null,
                new FindProductRequestDto(null, BigDecimal.ONE, BigDecimal.TEN, ProductStatus.AVAILABLE, 3L));
        String s = b.toString();
        assertTrue(s.contains("price >= 1"), s);
        assertTrue(s.contains("price <= 10"), s);
        assertTrue(s.contains("AVAILABLE"), s);
        // 카테고리는 서브쿼리로 걸리므로 toString 에는 in 절만 노출된다
        assertTrue(s.contains("product.brand.subCategory.id in"), s);
    }

    @Test
    void 조건이_전혀_없으면_전체_조회다() {
        assertEquals(new BooleanBuilder().toString(),
                ProductCustomRepositoryImpl.searchPredicate(null, null).toString());
    }
}
