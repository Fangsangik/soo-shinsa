package com.Soo_Shinsa.category.repository;

import com.Soo_Shinsa.category.dto.FindCategoryResponseDto;
import com.Soo_Shinsa.category.model.QCategory;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
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

        List<FindCategoryResponseDto> content = queryFactory
                .select(Projections.constructor(FindCategoryResponseDto.class,
                        category.id,
                        category.name,
                        Expressions.numberTemplate(Long.class, "COUNT(*) OVER()") // totalCount
                ))
                .from(category)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(category.count())
                .from(category)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }
}
