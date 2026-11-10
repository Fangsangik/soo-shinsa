package com.Soo_Shinsa.cartitem.repository;

import com.Soo_Shinsa.cartitem.dto.CartItemDateRequestDto;
import com.Soo_Shinsa.cartitem.dto.CartItemResponseDto;
import com.Soo_Shinsa.cartitem.model.QCartItem;
import com.Soo_Shinsa.product.dto.ProductOptionResponseDto;
import com.Soo_Shinsa.product.model.QProductOption;
import com.Soo_Shinsa.user.model.User;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;
import java.util.List;

@RequiredArgsConstructor
public class CartItemCustomRepositoryImpl implements CartItemCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CartItemResponseDto> findByAllCartItem(User user, CartItemDateRequestDto requestDto, Pageable pageable) {
        QCartItem cartItem = QCartItem.cartItem;
        QProductOption productOption = QProductOption.productOption;

        // 조건 빌더 생성
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(cartItem.user.userId.eq(user.getUserId()));

        if (requestDto.getStartDate() != null) {
            builder.and(cartItem.createdAt.goe(Timestamp.valueOf(requestDto.getStartDate().atStartOfDay())));
        }

        if (requestDto.getEndDate() != null) {
            builder.and(cartItem.createdAt.loe(Timestamp.valueOf(requestDto.getEndDate().atStartOfDay())));
        }

        // 페이징된 데이터 가져오기
        List<CartItemResponseDto> content = queryFactory
                .select(cartItem)
                .from(cartItem)
                .leftJoin(productOption).on(cartItem.product.id.eq(productOption.product.id))
                .where(builder)
                .orderBy(cartItem.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream()
                .map(cart -> {
                    // 상품 옵션 리스트 가져오기
                    List<ProductOptionResponseDto> optionDtos = queryFactory
                            .select(Projections.constructor(ProductOptionResponseDto.class,
                                    productOption.id,
                                    productOption.size,
                                    productOption.color,
                                    productOption.productStatus,
                                    productOption.product.id
                            ))
                            .from(productOption)
                            .where(productOption.id.eq(cart.getProductOption().getId()))
                            .fetch();

                    return new CartItemResponseDto(
                            cart.getId(),
                            cart.getQuantity(),
                            cart.getProduct().getId(),
                            cart.getProduct().getPrice(),
                            optionDtos
                    );
                })
                .toList();

        // 총 레코드 수 가져오기
        Long totalCount = queryFactory
                .select(cartItem.count())
                .from(cartItem)
                .where(builder)
                .fetchOne();

        // PageImpl로 결과 반환
        return new PageImpl<>(content, pageable, totalCount != null ? totalCount : 0);
    }

}
