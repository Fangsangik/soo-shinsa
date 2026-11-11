package com.Soo_Shinsa.product.repository;

import com.Soo_Shinsa.category.model.QSubCategory;
import com.Soo_Shinsa.product.dto.FindProductRequestDto;
import com.Soo_Shinsa.product.dto.ProductResponseDto;
import com.Soo_Shinsa.product.model.QProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class ProductCustomRepositoryImpl implements ProductCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ProductResponseDto> findAllProduct(Long brandId, FindProductRequestDto requestDto, Pageable pageable) {
        QProduct product = QProduct.product;
        QSubCategory subCategory = QSubCategory.subCategory;

        BooleanBuilder builder = new BooleanBuilder();
        if (brandId != null) {
            builder.and(product.brand.id.eq(brandId));
        }

        if (requestDto.getNameKeyword() != null && !requestDto.getNameKeyword().isEmpty()) {
            builder.and(product.name.containsIgnoreCase(requestDto.getNameKeyword()));
        }

        if (requestDto.getMinPrice() != null) {
            builder.and(product.price.goe(requestDto.getMinPrice()));
        }

        if (requestDto.getMaxPrice() != null) {
            builder.and(product.price.loe(requestDto.getMaxPrice()));
        }

        // count 쿼리는 행이 1개뿐이라 .fetch().size() 는 항상 1 → 페이지가 넘어가면 totalElements 가 틀렸다. fetchOne 으로 수정.
        Long total = queryFactory.select(product.count()).from(product).where(builder).fetchOne();
        return new PageImpl<>(
                queryFactory
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
                        .leftJoin(subCategory).on(product.brand.subCategory.id.eq(subCategory.id))
                        .where(builder)
                        .orderBy(product.price.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch(),
                pageable,
                total != null ? total : 0
        );
    }
}
