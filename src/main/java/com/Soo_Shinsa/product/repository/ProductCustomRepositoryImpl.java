package com.Soo_Shinsa.product.repository;

import com.Soo_Shinsa.product.dto.FindProductRequestDto;
import com.Soo_Shinsa.product.dto.ProductResponseDto;
import com.Soo_Shinsa.category.model.QSubCategory;
import com.Soo_Shinsa.product.model.QProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class ProductCustomRepositoryImpl implements ProductCustomRepository {

    /** MySQL ngram 파서의 기본 토큰 길이 */
    private static final int NGRAM_TOKEN_SIZE = 2;

    private final JPAQueryFactory queryFactory;

    /**
     * 상품 검색 조건 조립.
     * brandId가 null이면 브랜드 제한 없이 전체에서 검색한다.
     */
    static BooleanBuilder searchPredicate(Long brandId, FindProductRequestDto dto) {
        QProduct product = QProduct.product;
        BooleanBuilder builder = new BooleanBuilder();

        if (brandId != null) {
            builder.and(product.brand.id.eq(brandId));
        }
        if (dto == null) {
            return builder;
        }
        if (dto.getNameKeyword() != null && !dto.getNameKeyword().isBlank()) {
            builder.and(nameMatches(product, dto.getNameKeyword().trim()));
        }
        if (dto.getMinPrice() != null) {
            builder.and(product.price.goe(dto.getMinPrice()));
        }
        if (dto.getMaxPrice() != null) {
            builder.and(product.price.loe(dto.getMaxPrice()));
        }
        if (dto.getStatus() != null) {
            builder.and(product.productStatus.eq(dto.getStatus()));
        }
        if (dto.getCategoryId() != null) {
            // QProduct의 경로 초기화 깊이 제한으로 brand.subCategory.category 를 직접 탈 수 없어 서브쿼리로 처리
            QSubCategory subCategory = QSubCategory.subCategory;
            builder.and(product.brand.subCategory.id.in(
                    JPAExpressions.select(subCategory.id)
                            .from(subCategory)
                            .where(subCategory.category.id.eq(dto.getCategoryId()))));
        }
        return builder;
    }

    /**
     * 상품명 검색 조건.
     *
     * LIKE '%키워드%' 는 인덱스를 타지 못해 전체 스캔이므로 FULLTEXT(ngram) 을 쓴다.
     * ngram 토큰 최소 길이가 2라 한 글자는 잡히지 않으므로 그때만 LIKE 로 떨어뜨린다.
     */
    static BooleanExpression nameMatches(QProduct product, String keyword) {
        if (keyword.length() < NGRAM_TOKEN_SIZE) {
            return product.name.containsIgnoreCase(keyword);
        }
        // BOOLEAN MODE 연산자(+ - * " ( ) ~)를 무력화하려고 구문으로 감싼다
        String phrase = "\"" + keyword.replace("\"", " ") + "\"";
        return Expressions.numberTemplate(Double.class,
                "function('match_against', {0}, {1})", product.name, phrase).gt(0);
    }

    @Override
    public Page<ProductResponseDto> findAllProduct(Long brandId, FindProductRequestDto requestDto, Pageable pageable) {
        QProduct product = QProduct.product;
        BooleanBuilder builder = searchPredicate(brandId, requestDto);

        var content = queryFactory
                .select(Projections.constructor(ProductResponseDto.class,
                        product.id,
                        product.name,
                        product.price,
                        product.imageUrl,
                        product.productStatus,
                        product.brand.id,
                        product.brand.subCategory.id,
                        Expressions.numberTemplate(Long.class, "COUNT(*) OVER()")
                ))
                .from(product)
                .where(builder)
                .orderBy(product.price.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
