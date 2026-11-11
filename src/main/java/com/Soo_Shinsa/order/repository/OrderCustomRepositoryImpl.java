package com.Soo_Shinsa.order.repository;

import com.Soo_Shinsa.order.dto.OrderDateRequestDto;
import com.Soo_Shinsa.order.dto.OrderItemResponseDto;
import com.Soo_Shinsa.order.dto.OrdersResponseDto;
import com.Soo_Shinsa.order.model.QOrders;
import com.Soo_Shinsa.user.model.User;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class OrderCustomRepositoryImpl implements OrderCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<OrdersResponseDto> getAllByUserId(User user, OrderDateRequestDto dateRequestDto, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder().and(QOrders.orders.user.userId.eq(user.getUserId()));
        return fetchPage(builder, dateRequestDto, pageable);
    }

    @Override
    public Page<OrdersResponseDto> getAllForStaff(Long vendorUserId, com.Soo_Shinsa.constant.OrdersStatus status, OrderDateRequestDto dateRequestDto, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        if (status != null) builder.and(QOrders.orders.status.eq(status));
        if (vendorUserId != null) {
            com.Soo_Shinsa.order.model.QOrderItem oi = com.Soo_Shinsa.order.model.QOrderItem.orderItem;
            com.Soo_Shinsa.product.model.QProduct p = com.Soo_Shinsa.product.model.QProduct.product;
            com.Soo_Shinsa.brand.model.QBrand b = com.Soo_Shinsa.brand.model.QBrand.brand;
            // QueryDSL 기본 경로 깊이(2) 때문에 oi.product.brand.user 는 null → 명시적 join
            builder.and(QOrders.orders.id.in(com.querydsl.jpa.JPAExpressions.select(oi.order.id).from(oi).join(oi.product, p).join(p.brand, b).where(b.user.userId.eq(vendorUserId))));
        }
        return fetchPage(builder, dateRequestDto, pageable);
    }

    private Page<OrdersResponseDto> fetchPage(BooleanBuilder builder, OrderDateRequestDto dateRequestDto, Pageable pageable) {
        QOrders orders = QOrders.orders;

        if (dateRequestDto.getStartDate() != null) {
            builder.and(orders.createdAt.goe(Timestamp.valueOf(dateRequestDto.getStartDate().atStartOfDay())));
        }

        if (dateRequestDto.getEndDate() != null) {
            builder.and(orders.createdAt.loe(Timestamp.valueOf(dateRequestDto.getEndDate().atStartOfDay())));
        }

        // 주문 리스트 조회
        List<OrdersResponseDto> content = queryFactory
                .select(orders)
                .from(orders)
                .where(builder)
                .orderBy(orders.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream()
                .map(order -> OrdersResponseDto.builder()
                        .id(order.getId())
                        .orderId(order.getOrderId())
                        .totalPrice(order.getTotalPrice())
                        .status(order.getStatus())
                        .userId(order.getUser().getUserId())
                        .userName(order.getUser().getName())
                        .orderItems(order.getOrderItems().stream().map(OrderItemResponseDto::toDto).collect(Collectors.toList())) // 변환 추가
                        .build()
                )
                .toList();

        // 총 개수 조회
        Long totalCount = queryFactory
                .select(orders.count())
                .from(orders)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, totalCount != null ? totalCount : 0);
    }

}
