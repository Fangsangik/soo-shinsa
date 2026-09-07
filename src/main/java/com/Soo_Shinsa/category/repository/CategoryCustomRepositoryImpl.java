package com.Soo_Shinsa.category.repository;

import com.Soo_Shinsa.brand.model.QBrand;
import com.Soo_Shinsa.category.dto.FindCategoryResponseDto;
import com.Soo_Shinsa.category.model.QCategory;
import com.Soo_Shinsa.category.model.QSubCategory;
import com.Soo_Shinsa.product.model.QProduct;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class CategoryCustomRepositoryImpl implements CategoryCustomRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<FindCategoryResponseDto> findAllCategories(Pageable pageable) {
        QCategory category = QCategory.category;
        QSubCategory subCategory = QSubCategory.subCategory;
        QBrand brand = QBrand.brand;
        QProduct product = QProduct.product;

        // totalCount 는 "그 카테고리에 속한 상품 수"다.
        // 예전에는 select count(category) from category 를 넣어서 모든 행에 카테고리 총개수가
        // 똑같이 박혔다. 이름이 뜻하는 값도 아니었고, 화면에서 쓸 수도 없었다.
        List<FindCategoryResponseDto> content = queryFactory
                .select(Projections.constructor(FindCategoryResponseDto.class,
                        category.id,
                        category.name,
                        JPAExpressions.select(product.count())
                                .from(product)
                                .join(product.brand, brand)
                                .join(brand.subCategory, subCategory)
                                .where(subCategory.category.id.eq(category.id))
                ))
                .from(category)
                .orderBy(category.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory.select(category.count()).from(category).fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }
}
