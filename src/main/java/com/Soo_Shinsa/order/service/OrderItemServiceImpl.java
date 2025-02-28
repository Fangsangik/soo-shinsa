package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.global.constant.ProductStatus;
import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.InternalServerException;
import com.Soo_Shinsa.global.utils.EntityValidator;
import com.Soo_Shinsa.order.dto.OrderDateRequestDto;
import com.Soo_Shinsa.order.dto.OrderItemRequestDto;
import com.Soo_Shinsa.order.dto.OrderItemResponseDto;
import com.Soo_Shinsa.order.model.OrderItem;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.order.repository.OrderItemRepository;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import com.Soo_Shinsa.product.repository.ProductOptionRepository;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.Soo_Shinsa.global.constant.OrdersStatus.BEFOREPAYMENT;
import static com.Soo_Shinsa.global.exception.ErrorCode.ONLY_BEFORE_PAYMENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final OrdersRepository ordersRepository;
    private final UserRepository userRepository;
    private final RedissonClient redissonClient;
    private final ProductOptionRepository productOptionRepository;


    //오더 아이템 생성
    @Transactional
    @Override
    public OrderItemResponseDto createOrderItem(OrderItemRequestDto requestDto, User user) {

        String lockKey = "lock:orderItem:" + requestDto.getOrderId(); // 쿠폰 ID 기반 락
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("현재 오더아이템 발급 요청이 많아 잠시 후 다시 시도해주세요.");
            }

            User findUser = userRepository.findByIdOrElseThrow(user.getUserId());
            Orders findOrder = ordersRepository.findByIdOrElseThrow(requestDto.getOrderId());

            if (!findOrder.getStatus().equals(BEFOREPAYMENT)) {
                throw new InternalServerException(ONLY_BEFORE_PAYMENT);
            }

            EntityValidator.validateAndOrders(findOrder, findUser.getUserId());
            Product product = productRepository.findByIdOrElseThrow(requestDto.getProductId());
            if (product.getProductStatus().equals(ProductStatus.SOLD_OUT) || product.getProductStatus().equals(ProductStatus.UNAVAILABLE)) {
                throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
            }

            ProductOption findProductOption = productOptionRepository.findByIdOrElseThrow(product.getId());

            findProductOption.updateQuantity(-requestDto.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                    .order(findOrder)
                    .product(product)
                    .quantity(requestDto.getQuantity())
                    .build();

            findOrder.addOrderItem(orderItem);

            productOptionRepository.save(findProductOption);

            OrderItem savedOrderItem = orderItemRepository.save(orderItem);


            ordersRepository.save(findOrder);

            return OrderItemResponseDto.toDto(savedOrderItem);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("오더아이템 생성 중 오류가 발생했습니다.", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

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
