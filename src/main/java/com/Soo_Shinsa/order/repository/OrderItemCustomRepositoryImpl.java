package com.Soo_Shinsa.order.repository;

import com.Soo_Shinsa.order.dto.OrderDateRequestDto;
import com.Soo_Shinsa.order.dto.OrderItemResponseDto;
import com.Soo_Shinsa.order.model.QOrderItem;
import com.Soo_Shinsa.order.model.QOrders;
import com.Soo_Shinsa.product.model.QProduct;
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
public class OrderItemCustomRepositoryImpl implements OrderItemCustomRepository{

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<OrderItemResponseDto> findByAll(User user, OrderDateRequestDto dateRequestDto, Pageable pageable) {
        QOrderItem orderItem = QOrderItem.orderItem;
        QOrders orders = QOrders.orders;
        QProduct product = QProduct.product;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(orders.user.userId.eq(user.getUserId()));

        if (dateRequestDto.getStartDate() != null) {
            builder.and(orders.createdAt.goe(Timestamp.valueOf(dateRequestDto.getStartDate().atStartOfDay())));
        }

        if (dateRequestDto.getEndDate() != null) {
            builder.and(orders.createdAt.loe(Timestamp.valueOf(dateRequestDto.getEndDate().atTime(23, 59, 59))));
        }

        // 실제 데이터를 가져옴
        List<OrderItemResponseDto> content = queryFactory
                .select(Projections.constructor(OrderItemResponseDto.class,
                        orderItem.id,
                        product.id,
                        product.name,
                        orderItem.quantity,
                        orderItem.price,          // 할인 전 가격
                        orderItem.discountPrice   // 할인된 가격 추가
                ))
                .from(orderItem)
                .leftJoin(orders).on(orderItem.order.id.eq(orders.id))
                .leftJoin(product).on(orderItem.product.id.eq(product.id))
                .where(builder)
                .orderBy(orderItem.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 총 개수 가져오기
        Long totalCount = queryFactory
                .select(orderItem.count())
                .from(orderItem)
                .leftJoin(orders).on(orderItem.order.id.eq(orders.id))
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, totalCount != null ? totalCount : 0);
    }

}
