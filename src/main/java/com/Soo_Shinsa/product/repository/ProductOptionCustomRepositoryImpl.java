package com.Soo_Shinsa.product.repository;

import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.Soo_Shinsa.product.dto.FindProductOptionRequestDto;
import com.Soo_Shinsa.product.dto.ProductOptionResponseDto;
import com.Soo_Shinsa.product.model.QProductOption;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class ProductOptionCustomRepositoryImpl implements ProductOptionCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ProductOptionResponseDto> findProductsByOptionalSizeAndColor(FindProductOptionRequestDto requestDto, Pageable pageable) {
        QProductOption productOption = QProductOption.productOption;

        if (requestDto.getColor() == null && requestDto.getSize() == null) {
            throw new InvalidInputException(ErrorCode.SELECT_COLOR_OR_SIZE);
        }

        // 조건 빌더 생성
        BooleanBuilder builder = new BooleanBuilder();

        if (requestDto.getColor() != null) {
            builder.and(productOption.color.eq(requestDto.getColor()));
        }

        if (requestDto.getSize() != null) {
            builder.and(productOption.size.eq(requestDto.getSize()));
        }

        // 페이징된 데이터 가져오기
        List<ProductOptionResponseDto> content = queryFactory
                .select(Projections.constructor(ProductOptionResponseDto.class,
                        productOption.id,
                        productOption.size,
                        productOption.color,
                        productOption.productStatus,
                        productOption.product.id
                ))
                .from(productOption)
                .where(builder)
                .orderBy(productOption.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 총 레코드 수 가져오기
        Long totalCount = queryFactory
                .select(productOption.count())
                .from(productOption)
                .where(builder)
                .fetchOne();

        // PageImpl로 결과 반환
        return new PageImpl<>(content, pageable, totalCount != null ? totalCount : 0);
    }
}
