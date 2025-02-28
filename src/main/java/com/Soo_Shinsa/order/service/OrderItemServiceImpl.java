package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.global.utils.EntityValidator;
import com.Soo_Shinsa.order.dto.OrderDateRequestDto;
import com.Soo_Shinsa.order.dto.OrderItemResponseDto;
import com.Soo_Shinsa.order.model.OrderItem;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.order.repository.OrderItemRepository;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final OrdersRepository ordersRepository;
    private final UserRepository userRepository;

    @Override
    public OrderItemResponseDto findById(Long orderItemsId, User user) {
        User findUser = userRepository.findByIdOrElseThrow(user.getUserId());
        OrderItem findOrderItem = orderItemRepository.findByIdOrElseThrow(orderItemsId);

        EntityValidator.validateAndOrderItem(findOrderItem, findUser.getUserId());
        return OrderItemResponseDto.toDto(findOrderItem);
    }


    //유저 오더아이템들을 찾아옴
    @Override
    public Page<OrderItemResponseDto> findByAll(User user, OrderDateRequestDto dateRequestDto, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderItemRepository.findByAll(user, dateRequestDto, pageable);
    }

    //오더 아이템 수정
    @Transactional
    @Override
    public OrderItemResponseDto update(Long orderItemsId, Integer quantity, User user) {
        EntityValidator.validateAdminAccess(user);
        User findUser = userRepository.findByIdOrElseThrow(user.getUserId());
        OrderItem findOrderItem = orderItemRepository.findByIdOrElseThrow(orderItemsId);


        EntityValidator.validateAndOrderItem(findOrderItem, findUser.getUserId());
        findOrderItem.updateOrderItem(quantity);
        OrderItem save = orderItemRepository.save(findOrderItem);
        return OrderItemResponseDto.toDto(save);
    }

    //오더 아이템 삭제
    @Transactional
    @Override
    public OrderItemResponseDto delete(Long orderItemsId, User user) {
        User findUser = userRepository.findByIdOrElseThrow(user.getUserId());

        OrderItem findOrderItem = orderItemRepository.findByIdOrElseThrow(orderItemsId);
        EntityValidator.validateAndOrderItem(findOrderItem, findUser.getUserId());

        Orders order = ordersRepository.findByIdOrElseThrow(findOrderItem.getOrder().getId());

        order.removeOrderItem(findOrderItem); // 연관 관계에서 제거
        ordersRepository.delete(order);// Order 저장 (OrderItem 자동 삭제)

        return OrderItemResponseDto.toDto(findOrderItem);

    }
}
