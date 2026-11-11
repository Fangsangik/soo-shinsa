package com.Soo_Shinsa.order.repository;

import com.Soo_Shinsa.order.dto.OrderDateRequestDto;
import com.Soo_Shinsa.order.dto.OrdersResponseDto;
import com.Soo_Shinsa.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderCustomRepository {
    Page<OrdersResponseDto> getAllByUserId(User user, OrderDateRequestDto dateRequestDto, Pageable pageable);

    /** 관리자/입점사 주문 목록. vendorUserId 가 null 이면 전체(ADMIN), 아니면 해당 입점사 브랜드 상품이 포함된 주문만. */
    Page<OrdersResponseDto> getAllForStaff(Long vendorUserId, com.Soo_Shinsa.constant.OrdersStatus status, OrderDateRequestDto dateRequestDto, Pageable pageable);
}
