package com.Soo_Shinsa.cartitem.repository;

import com.Soo_Shinsa.cartitem.dto.CartItemDateRequestDto;
import com.Soo_Shinsa.cartitem.dto.CartItemResponseDto;
import com.Soo_Shinsa.cartitem.model.QCartItem;
import com.Soo_Shinsa.cartitem.model.QCartItemProductOption;
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
import java.util.Optional;

@RequiredArgsConstructor
public class CartItemCustomRepositoryImpl implements CartItemCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CartItemResponseDto> findByAllCartItem(User user, CartItemDateRequestDto requestDto, Pageable pageable) {
        QCartItem cartItem = QCartItem.cartItem;
        QCartItemProductOption cartItemProductOption = QCartItemProductOption.cartItemProductOption;
        QProductOption productOption = QProductOption.productOption;

        // 조건 빌더 생성
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(cartItem.user.userId.eq(user.getUserId()));

        if (requestDto.getStartDate() != null) {
            builder.and(cartItem.createdAt.goe(Timestamp.valueOf(requestDto.getStartDate().atStartOfDay())));
        }
        if (requestDto.getEndDate() != null) {
            builder.and(cartItem.createdAt.loe(Timestamp.valueOf(requestDto.getEndDate().atTime(23, 59, 59))));
        }

        // ✅ 단일 쿼리로 장바구니 + 옵션 가져오기 (JOIN FETCH)
        List<CartItemResponseDto> content = queryFactory
                .select(Projections.constructor(CartItemResponseDto.class,
                        cartItem.id,
                        cartItem.quantity,
                        cartItem.product.id,
                        cartItem.product.price,
                        Projections.list(
                                Projections.constructor(ProductOptionResponseDto.class,
                                        productOption.id,
                                        productOption.size,
                                        productOption.color,
                                        productOption.productStatus,
                                        productOption.product.id,
                                        productOption.quantity
                                )
                        )
                ))
                .from(cartItem)
                .leftJoin(cartItemProductOption).on(cartItem.id.eq(cartItemProductOption.cartItem.id))  // ✅ 장바구니-상품옵션 관계 조인
                .leftJoin(productOption).on(cartItemProductOption.productOption.id.eq(productOption.id)) // ✅ 상품 옵션 조인
                .where(builder)
                .orderBy(cartItem.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // ✅ `COUNT(*) OVER()` 활용하여 쿼리 1회만 실행
        long totalCount = Optional.ofNullable(
                queryFactory
                        .select(cartItem.count())
                        .from(cartItem)
                        .where(builder)
                        .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(content, pageable, totalCount);

    }

}
