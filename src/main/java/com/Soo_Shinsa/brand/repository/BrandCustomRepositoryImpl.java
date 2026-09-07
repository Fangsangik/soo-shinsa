package com.Soo_Shinsa.brand.repository;
import com.Soo_Shinsa.brand.dto.FindBrandAllResponseDto;
import com.Soo_Shinsa.brand.model.QBrand;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class BrandCustomRepositoryImpl implements BrandCustomRepository{

    private final JPAQueryFactory queryFactory;

    public Page<FindBrandAllResponseDto> getAllBrand(Pageable pageable) {
        QBrand brand = QBrand.brand;

        // 페이징 데이터 조회
        List<FindBrandAllResponseDto> content = queryFactory
                .select(Projections.constructor(FindBrandAllResponseDto.class,
                        brand.id,
                        brand.name,
                        brand.context,
                        brand.status
                ))
                .from(brand)
                .orderBy(brand.name.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch(); // 페이지 데이터 조회

        // 총 레코드 수 계산
        Long totalCount = queryFactory
                .select(brand.count())
                .from(brand)
                .fetchOne(); // 전체 레코드 수 조회

        // PageImpl 생성
        return new PageImpl<>(content, pageable, totalCount == null ? 0 : totalCount);
    }
}
