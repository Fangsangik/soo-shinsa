package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.cartitem.model.CartItem;
import com.Soo_Shinsa.cartitem.repository.CartItemRepository;
import com.Soo_Shinsa.category.repository.CartItemProductOptionRepository;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponUser;
import com.Soo_Shinsa.coupon.repository.CouponRepository;
import com.Soo_Shinsa.coupon.repository.CouponUserRepository;
import com.Soo_Shinsa.global.constant.OrdersStatus;
import com.Soo_Shinsa.global.constant.ProductStatus;
import com.Soo_Shinsa.global.constant.TossPayStatus;
import com.Soo_Shinsa.global.config.BusinessMetrics;
import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.InternalServerException;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.Soo_Shinsa.global.exception.NotFoundException;
import com.Soo_Shinsa.global.utils.EntityValidator;
import com.Soo_Shinsa.order.dto.*;
import com.Soo_Shinsa.order.model.OrderItem;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.order.model.Payment;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import com.Soo_Shinsa.order.repository.PaymentRepository;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import com.Soo_Shinsa.product.repository.ProductOptionRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdersServiceImpl implements OrdersService {

    /** 이 시간이 지나도 결제되지 않은 주문은 되돌린다 */
    private final BusinessMetrics metrics;

    @Value("${app.order.pending-timeout:PT30M}")
    private Duration pendingTimeout = Duration.ofMinutes(30);

    private final OrdersRepository ordersRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final CouponUserRepository couponUserRepository;
    private final ProductOptionRepository productOptionRepository;
    private final CouponRepository couponRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentsService tossPaymentsService;
    private final CartItemProductOptionRepository cartItemProductOptionRepository;
    private final OrderCacheService orderCacheService;


    @Override
    public OrdersResponseDto getOrderById(Long orderId, User user) {
        User findUser = userRepository.findByIdOrElseThrow(user.getUserId());

        Orders findOrder = ordersRepository.findByIdOrElseThrow(orderId);

        EntityValidator.validateAndOrders(findOrder, findUser.getUserId());


        return OrdersResponseDto.toDto(findOrder);
    }

    @Override
    public Page<OrdersResponseDto> getAllByUserId(User user, OrderDateRequestDto dateRequestDto, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ordersRepository.getAllByUserId(user, dateRequestDto, pageable);
    }


    //    단일 상품 구매
    //    상품을 찾아와서 주문번호를 생성 후 주문을 만들고 거기에 주문아이템에 물건을 담음
    // 재고는 decreaseStock 의 조건부 원자 UPDATE 가 DB 행 락으로 직렬화한다. 분산락 불필요.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public OrdersResponseDto createSingleProductOrder(User user, Long productOptionId, Integer quantity) {

        log.info("🔒 StockLock 적용 확인 - productOptionId: {}", productOptionId);

        // DB 락 제거, 분산락으로만 동시성 제어
        ProductOption productOption = productOptionRepository.findByIdOrElseThrow(productOptionId);
        log.info("🛒 현재 재고: {}", productOption.getQuantity());

        Product product = productOption.getProduct();
        if (product.getProductStatus().equals(ProductStatus.SOLD_OUT) || product.getProductStatus().equals(ProductStatus.UNAVAILABLE)) {
            throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        // 원자적 업데이트로 경쟁 조건 방지
        int updatedRows = productOptionRepository.decreaseStock(productOptionId, quantity);
        if (updatedRows == 0) {
            log.error("🚨 재고 부족으로 주문 실패 - 상품 옵션 ID: {}, 요청 수량: {}", productOptionId, quantity);
            metrics.orderStockShortage();
            throw new InvalidInputException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }
        
        // decreaseStock 이 판매수량까지 함께 올린다.
        // 여기서 예전 엔티티를 다시 save 하면 방금 차감한 재고가 stale 값으로 덮어써진다.
        log.info("📉 재고 감소 완료 - 상품 옵션 ID: {}", productOptionId);

        productOption = productOptionRepository.findByIdOrElseThrow(productOptionId);
        BigDecimal totalPrice = productOption.getProduct().getPrice().multiply(BigDecimal.valueOf(quantity));


        Orders order = Orders.builder()
                .user(user)
                .totalPrice(totalPrice)
                .status(OrdersStatus.PENDING)
                .build();


        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .productOption(productOption)
                .price(order.getTotalPrice())
                .quantity(quantity)
                .build();

        order.addOrderItem(orderItem);

        ordersRepository.save(order);

        return OrdersResponseDto.toDto(order);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public OrdersResponseDto createSingleOrderCartItem(User user, OrderCreateRequestDto requestDto) {
        CartItem cartItem = cartItemRepository.findByIdOrElseThrow(requestDto.getCartId());

        // 여러 행을 차감하므로 항상 같은 순서로 잠가야 동시 주문끼리 데드락이 나지 않는다
        List<ProductOption> productOptions = cartItem.getProductOptions().stream()
                .map(cartItemProductOption -> productOptionRepository.findByIdOrElseThrow(cartItemProductOption.getProductOption().getId()))
                .sorted(Comparator.comparing(ProductOption::getId))
                .toList();

        Product product = cartItem.getProduct();

        if (product.getProductStatus().equals(ProductStatus.SOLD_OUT) || product.getProductStatus().equals(ProductStatus.UNAVAILABLE)) {
            throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        // 읽고-검사하고-쓰는 방식은 동시에 들어오면 서로의 차감을 덮어쓴다(lost update).
        // 다른 주문 경로와 동일하게 조건부 원자 UPDATE 로 차감한다.
        for (ProductOption option : productOptions) {
            int updatedRows = productOptionRepository.decreaseStock(option.getId(), cartItem.getQuantity());
            if (updatedRows == 0) {
                log.error("🚨 재고 부족으로 주문 실패 - 상품 옵션 ID: {}, 요청 수량: {}", option.getId(), cartItem.getQuantity());
                throw new InvalidInputException(ErrorCode.CAN_NOT_USE_PRODUCT);
            }
        }

        // 예외를 삼키면 쿠폰이 소진되지 않은 채 할인가로 결제되거나,
        // 반대로 할인 없이 결제된다. 실패하면 주문을 중단한다.
        isUsedCoupon(user, cartItem);

        // ✅ 총 가격을 0으로 초기화
        BigDecimal totalPrice = BigDecimal.ZERO;

        Orders order = Orders.builder()
                .user(user)
                .totalPrice(totalPrice)  // 초기 0으로 설정
                .status(OrdersStatus.PENDING)
                .build();

        // ✅ 각 옵션별로 가격을 계산하고, `totalPrice`에 반영
        for (ProductOption option : productOptions) {
            // ✅ 할인된 가격이 있으면 적용, 없으면 `null`
            BigDecimal optionPrice = (cartItem.getDiscountedPrice() != null)
                    ? cartItem.getDiscountedPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                    : option.getProduct().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            totalPrice = totalPrice.add(optionPrice); // 🚀 **옵션 가격을 누적**

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .price(option.getProduct().getPrice())
                    .discountPrice(cartItem.getDiscountedPrice() != null ? optionPrice : null) // ✅ 할인 없으면 `null`
                    .productOption(option)
                    .product(option.getProduct())
                    .quantity(cartItem.getQuantity())
                    .build();

            order.addOrderItem(orderItem);
        }

        // ✅ 주문 객체에 최종 가격 업데이트 (JPA `save()` 전에 반영)
        order.updateTotalPrice(totalPrice);

        ordersRepository.save(order); // ✅ 최종 가격 업데이트된 주문 저장
        cartItemRepository.delete(cartItem);
        if (cartItemProductOptionRepository.existsByCartItemId(cartItem.getId())) {
            cartItemProductOptionRepository.deleteByCartItemId(cartItem.getId());
        }


        return OrdersResponseDto.toDto(order);
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public OrdersResponseDto createAllOrderFromCart(User user) {
        List<CartItem> cartItems = cartItemRepository.findByUserUserIdWithLock(user.getUserId());

        if (cartItems.isEmpty()) {
            throw new NotFoundException(ErrorCode.NOT_FOUND_CART);
        }

        BigDecimal totalPrice = BigDecimal.ZERO;

        Orders order = Orders.builder()
                .user(user)
                .totalPrice(totalPrice)
                .status(OrdersStatus.PENDING)
                .build();

        for (CartItem cartItem : cartItems) {
            log.info("🔍 주문 처리 중 - 카트 아이템 ID: {}, 쿠폰 적용 여부: {}", cartItem.getId(), cartItem.getCoupon() != null);

            List<ProductOption> productOptions = cartItem.getProductOptions().stream()
                    .map(cartItemProductOption -> productOptionRepository.findByIdOrElseThrow(cartItemProductOption.getProductOption().getId()))
                    .sorted(Comparator.comparing(ProductOption::getId))
                    .toList();

            Integer quantity = cartItem.getQuantity();

            BigDecimal cartItemTotalPrice = BigDecimal.ZERO; // **각 CartItem별 개별 가격 계산**

            for (ProductOption option : productOptions) {
                int updatedRows = productOptionRepository.decreaseStock(option.getId(), quantity);
                if (updatedRows == 0) {
                    log.error("🚨 재고 부족으로 주문 실패 - 상품 옵션 ID: {}, 요청 수량: {}", option.getId(), quantity);
                    throw new InvalidInputException(ErrorCode.CAN_NOT_USE_PRODUCT);
                }
                // decreaseStock 이 salesCount 까지 처리한다 (stale 엔티티 재저장 금지)
            }

            log.info("🎟 쿠폰 적용 시도 - 카트 아이템 ID: {}", cartItem.getId());
            isUsedCoupon(user, cartItem);

            for (ProductOption option : productOptions) {
                BigDecimal optionPrice = option.getProduct().getPrice().multiply(BigDecimal.valueOf(quantity));
                BigDecimal discountPrice = cartItem.getDiscountedPrice() != null
                        ? cartItem.getDiscountedPrice().multiply(BigDecimal.valueOf(quantity))
                        : optionPrice;

                cartItemTotalPrice = cartItemTotalPrice.add(discountPrice); // **각 카트 아이템의 가격을 누적**

                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .price(option.getProduct().getPrice())
                        .discountPrice(discountPrice)
                        .product(option.getProduct())
                        .productOption(option)
                        .quantity(quantity)
                        .build();

                order.addOrderItem(orderItem);
                log.info("✅ 주문 아이템 생성 완료 - 상품 옵션 ID: {}, 남은 재고: {}", option.getId(), option.getQuantity());
            }

            totalPrice = totalPrice.add(cartItemTotalPrice);

            order.updateTotalPrice(totalPrice);
            ordersRepository.save(order);

            return OrdersResponseDto.toDto(order);
        }


        order.updateTotalPrice(totalPrice); // ✅ 총 주문 가격 업데이트
        List<Long> cartItemIds = cartItems.stream().map(CartItem::getId).toList();

        ordersRepository.save(order);
        cartItemProductOptionRepository.deleteByCartItemIds(cartItemIds);
        cartItemRepository.deleteAllInBatch(cartItems);

        log.info("✅ 전체 주문 완료 - 최종 재고 확인");
        return OrdersResponseDto.toDto(order);
    }


    private void isUsedCoupon(User user, CartItem cartItem) {
        if (cartItem.getCoupon() == null) {
            log.warn("⚠️ 쿠폰이 적용되지 않은 카트 아이템입니다. 카트 아이템 ID: {}", cartItem.getId());
            return;  // 쿠폰이 없는 경우 바로 종료
        }

        // DB 락 제거, 분산락으로 동시성 제어
        Coupon coupon = couponRepository.findByIdOrElseThrow(cartItem.getCoupon().getId());

        log.info("🎟 쿠폰 사용 검증 시작 - 쿠폰 ID: {}, 남은 수량: {}", coupon.getId(), coupon.getRemainingCount());

        CouponUser couponUser = couponUserRepository.findByCouponIdAndUserUserId(coupon.getId(), user.getUserId())
                .orElseThrow(() -> new InvalidInputException(ErrorCode.NOT_FOUND_COUPON));

        // 읽고-검사하고-쓰는 대신 조건부 UPDATE 로 처리한다.
        // 0행이면 다른 요청이 먼저 썼다는 뜻이므로 그대로 실패시킨다.
        if (couponUserRepository.markAsUsed(couponUser.getId(), LocalDate.now()) == 0) {
            log.error("🚨 이미 사용된 쿠폰 - 쿠폰 ID: {}", coupon.getId());
            throw new InvalidInputException(ErrorCode.ALREADY_USED_COUPON);
        }

        if (couponRepository.decreaseRemainingCount(coupon.getId(), 1) == 0) {
            log.error("🚨 쿠폰 사용 불가 - 쿠폰 ID: {}, 재고 부족", coupon.getId());
            throw new InvalidInputException(ErrorCode.COUPON_OUT_OF_STOCK);
        }
        log.info("✅ 쿠폰 사용 완료 - 쿠폰 ID: {}", coupon.getId());

        // 쿠폰 적용 후 카트 아이템 업데이트 (위 UPDATE 들이 컨텍스트를 비우므로 다시 읽는다)
        CartItem managedCartItem = cartItemRepository.findByIdOrElseThrow(cartItem.getId());
        managedCartItem.applyCoupon(couponRepository.findByIdOrElseThrow(coupon.getId()));
        cartItemRepository.saveAndFlush(managedCartItem);
        log.info("✅ 카트 아이템 업데이트 완료 - 카트 아이템 ID: {}", cartItem.getId());
    }


//    @Transactional
//    public void decreaseProductQuantity(Long productOptionId, Integer quantity) {
//        ProductOption productOption = productOptionRepository.findByIdWithLock(productOptionId)
//                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_PRODUCT_OPTION));
//
//        log.info("🔽 [재고 차감 요청] 상품 ID: {}, 현재 재고: {}, 요청 수량: {}",
//                productOptionId, productOption.getQuantity(), quantity);
//
//        if (productOption.getQuantity() < quantity) {
//            log.error("🚨 재고 부족으로 주문 실패 - 상품 ID: {}, 요청 수량: {}, 남은 재고: {}",
//                    productOptionId, quantity, productOption.getQuantity());
//            throw new InvalidInputException(ErrorCode.CAN_NOT_USE_PRODUCT);
//        }
//
//        productOption.decreaseQuantity(quantity);
//        productOptionRepository.saveAndFlush(productOption);
//
//        log.info("✅ 재고 차감 완료 - 상품 ID: {}, 남은 재고: {}", productOptionId, productOption.getQuantity());
//    }


    @Transactional
    @Override
    public OrdersResponseDto updateOrder(User user, Long orderId, OrdersStatus status) {

        User findUser = userRepository.findByIdOrElseThrow(user.getUserId());
        Orders findOrder = ordersRepository.findByIdOrElseThrow(orderId);

        EntityValidator.validateAndOrders(findOrder, findUser.getUserId());
        findOrder.updateStatus(status);
        Orders savedOrder = ordersRepository.save(findOrder);
        return OrdersResponseDto.toDto(savedOrder);
    }

    @Transactional
    public void cancelOrder(User user, Long orderId) throws JsonProcessingException {
        // 사용자 및 주문 조회
        User findUser = userRepository.findByIdOrElseThrow(user.getUserId());
        Orders findOrder = ordersRepository.findByIdOrElseThrow(orderId);

        // 주문이 해당 사용자에게 속하는지 검증
        EntityValidator.validateAndOrders(findOrder, findUser.getUserId());

        if (findOrder.getStatus() == OrdersStatus.ORDERCANCEL) {
            throw new InvalidInputException(ErrorCode.ALREADY_CANCEL_ORDER);
        }

        // 결제 정보 조회
        Payment payment = paymentRepository.findByOrderId(findOrder.getOrderId());

        if (payment != null && payment.getStatus() == TossPayStatus.PAYMENT) {
            // 결제 완료 상태라면 결제 취소 실행
            tossPaymentsService.cancelPayment(payment.getPaymentKey(), CANCEL_REASON);
        }

        // 재고 복원. 부분 취소만 복원하고 전체 취소는 하지 않아 재고가 사라지고 있었다.
        // 이미 취소된 아이템은 부분 취소 때 복원했으므로 건너뛴다.
        findOrder.getOrderItems().stream()
                .filter(orderItem -> !orderItem.isCancelled())
                .forEach(orderItem -> {
                    restoreStock(orderItem);
                    orderItem.cancelOrderItem(CANCEL_REASON);
                });

        // 주문 상태 변경
        findOrder.updateStatus(OrdersStatus.ORDERCANCEL);
        ordersRepository.save(findOrder);

        orderCacheService.evictOrderCaches(findOrder.getId(), findUser.getUserId());
    }

    @Transactional
    @Override
    public PartialCancelResponseDto partialCancelOrder(User user, Long orderId, PartialCancelRequestDto requestDto) throws JsonProcessingException {
        // 사용자 및 주문 검증
        User findUser = userRepository.findByIdOrElseThrow(user.getUserId());
        Orders findOrder = ordersRepository.findByIdOrElseThrow(orderId);
        EntityValidator.validateAndOrders(findOrder, findUser.getUserId());

        // 주문이 취소 가능한 상태인지 확인
        if (findOrder.getStatus() == OrdersStatus.ORDERCANCEL) {
            throw new InvalidInputException(ErrorCode.ALREADY_CANCEL_ORDER);
        }

        // 취소할 주문 아이템들 조회 및 검증
        List<OrderItem> orderItemsToCancel = findOrder.getOrderItems().stream()
                .filter(item -> requestDto.getOrderItemIds().contains(item.getId()))
                .toList();

        if (orderItemsToCancel.isEmpty()) {
            throw new NotFoundException(ErrorCode.NOT_FOUND_ORDER_ITEM);
        }

        // 취소 불가능한 아이템 확인
        List<OrderItem> nonCancellableItems = orderItemsToCancel.stream()
                .filter(item -> !item.isCancellable())
                .toList();

        if (!nonCancellableItems.isEmpty()) {
            throw new InvalidInputException(ErrorCode.CAN_NOT_CANCEL_ORDER);
        }

        // 취소할 총 금액 계산
        BigDecimal totalCancelAmount = orderItemsToCancel.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 주문 아이템 취소 처리
        List<PartialCancelResponseDto.CancelledOrderItemDto> cancelledItemDtos = orderItemsToCancel.stream()
                .map(item -> {
                    // 재고 복원
                    restoreStock(item);
                    
                    // 주문 아이템 취소
                    item.cancelOrderItem(requestDto.getCancelReason());
                    
                    return PartialCancelResponseDto.CancelledOrderItemDto.builder()
                            .orderItemId(item.getId())
                            .productName(item.getProduct().getName())
                            .optionName(item.getProductOption().getSize() + "/" + item.getProductOption().getColor())
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .totalPrice(item.getTotalPrice())
                            .cancelReason(requestDto.getCancelReason())
                            .build();
                })
                .collect(Collectors.toList());

        // 부분 결제 취소 처리
        String refundStatus = processPartialRefund(findOrder, totalCancelAmount, requestDto.getCancelReason());

        // 모든 주문 아이템이 취소된 경우 주문 전체를 취소 상태로 변경
        boolean allItemsCancelled = findOrder.getOrderItems().stream()
                .allMatch(OrderItem::isCancelled);
        
        if (allItemsCancelled) {
            findOrder.updateStatus(OrdersStatus.ORDERCANCEL);
        }

        ordersRepository.save(findOrder);

        // 🚀 캐시 무효화 (부분 취소로 인한 데이터 변경)
        orderCacheService.evictOrderCaches(findOrder.getId(), findUser.getUserId());
        log.info("🗑️ 부분 취소로 인한 캐시 무효화 완료 - 주문 ID: {}, 사용자 ID: {}", findOrder.getId(), findUser.getUserId());

        return PartialCancelResponseDto.builder()
                .orderId(findOrder.getId())
                .orderNumber(findOrder.getOrderId())
                .cancelledItems(cancelledItemDtos)
                .totalCancelledAmount(totalCancelAmount)
                .refundAmount(totalCancelAmount)
                .refundStatus(refundStatus)
                .message("선택된 상품의 부분 취소가 완료되었습니다.")
                .build();
    }

    private static final String CANCEL_REASON = "사용자 주문 취소";
    private static final String EXPIRE_REASON = "결제 시간 초과";

    /**
     * 결제되지 않은 채 방치된 주문을 되돌린다.
     *
     * 주문 생성 시점에 재고를 차감하는데 결제하지 않은 주문을 되돌리는 경로가 없어서,
     * 장바구니만 담고 이탈하면 재고가 영영 묶였다.
     * 결제가 완료된 주문은 상태 반영이 늦은 것일 수 있으므로 건드리지 않는다.
     */
    @Transactional
    @Override
    public int expirePendingOrders(int batchSize) {
        Timestamp threshold = Timestamp.from(Instant.now().minus(pendingTimeout));
        List<Orders> expired = ordersRepository.findExpired(
                OrdersStatus.PENDING, threshold, PageRequest.of(0, Math.max(batchSize, 1)));

        int reverted = 0;
        for (Orders order : expired) {
            Payment payment = paymentRepository.findByOrderId(order.getOrderId());
            if (payment != null && payment.getStatus() == TossPayStatus.PAYMENT) {
                log.warn("결제는 완료됐는데 주문이 PENDING 입니다. 만료 대상에서 제외 - 주문 ID: {}", order.getId());
                continue;
            }

            order.getOrderItems().stream()
                    .filter(orderItem -> !orderItem.isCancelled())
                    .forEach(orderItem -> {
                        restoreStock(orderItem);
                        orderItem.cancelOrderItem(EXPIRE_REASON);
                    });
            order.updateStatus(OrdersStatus.ORDERCANCEL);
            ordersRepository.save(order);
            orderCacheService.evictOrderCaches(order.getId(), order.getUser().getUserId());
            reverted++;
        }

        if (reverted > 0) {
            metrics.orderExpired(reverted);
            log.info("미결제 주문 {}건 취소, 재고 반환 완료", reverted);
        }
        return reverted;
    }


    private void restoreStock(OrderItem orderItem) {
        // 재고 복원
        ProductOption productOption = orderItem.getProductOption();
        int updatedRows = productOptionRepository.increaseStock(productOption.getId(), orderItem.getQuantity());
        
        if (updatedRows == 0) {
            log.warn("재고 복원 실패 - 상품 옵션 ID: {}, 복원 수량: {}", 
                     productOption.getId(), orderItem.getQuantity());
        } else {
            log.info("재고 복원 완료 - 상품 옵션 ID: {}, 복원 수량: {}", 
                     productOption.getId(), orderItem.getQuantity());
        }
    }

    private String processPartialRefund(Orders order, BigDecimal refundAmount, String cancelReason) throws JsonProcessingException {
        Payment payment = paymentRepository.findByOrderId(order.getOrderId());
        
        if (payment == null) {
            return "결제 정보 없음";
        }

        if (payment.getStatus() == TossPayStatus.PAYMENT) {
            try {
                // Toss Payments 부분 취소 API 호출
                tossPaymentsService.partialCancelPayment(
                    payment.getPaymentKey(), 
                    refundAmount, 
                    cancelReason
                );
                return "환불 진행 중";
            } catch (Exception e) {
                log.error("부분 환불 처리 실패: {}", e.getMessage());
                return "환불 실패";
            }
        }

        return "환불 불가";
    }

    // 🚀 성능 최적화된 주문 상세 조회 (N+1 해결)
    @Override
    @Transactional(readOnly = true)
    public OrdersResponseDto getOrderByIdOptimized(Long orderId, User user) {
        log.info("🚀 최적화된 주문 상세 조회 시작 - 주문 ID: {}, 사용자 ID: {}", orderId, user.getUserId());
        
        User findUser = userRepository.findByIdOrElseThrow(user.getUserId());

        // Fetch Join을 사용하여 한 번의 쿼리로 모든 데이터 조회
        Orders findOrder = ordersRepository.findByIdWithItemsAndUser(orderId, findUser.getUserId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_ORDER));

        log.info("✅ 최적화된 주문 조회 완료 - 단일 쿼리로 모든 데이터 로드");
        return OrdersResponseDto.toDto(findOrder);
    }

    // 🚀 성능 최적화된 주문 목록 조회 (경량 DTO 사용 + 캐싱)
    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> getOrderSummariesByUserId(User user, OrderDateRequestDto dateRequestDto, int page, int size) {
        log.info("🚀 최적화된 주문 요약 목록 조회 시작 - 사용자 ID: {}, 페이지: {}, 크기: {}", user.getUserId(), page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        Page<OrderSummaryDto> orderSummaries;
        
        // 날짜 조건이 있으면 캐시 사용 안함 (변동이 큼)
        if (dateRequestDto.getStartDate() != null || dateRequestDto.getEndDate() != null) {
            orderSummaries = orderCacheService.getCachedOrderSummariesByUserIdAndDate(
                user.getUserId(),
                dateRequestDto.getStartDate() != null ? dateRequestDto.getStartDate().atStartOfDay() : null,
                dateRequestDto.getEndDate() != null ? dateRequestDto.getEndDate().atTime(23, 59, 59) : null,
                pageable
            );
        } else {
            // 기본 조회는 캐시 사용
            orderSummaries = orderCacheService.getCachedOrderSummariesByUserId(
                user.getUserId(),
                pageable
            );
        }
        
        log.info("✅ 최적화된 주문 요약 조회 완료 - {} 건 조회됨 (캐시 적용)", orderSummaries.getContent().size());
        return orderSummaries;
    }

    // 🚀 성능 최적화된 주문 목록 조회 (2단계 조회 방식)
    @Override
    @Transactional(readOnly = true)
    public Page<OrdersResponseDto> getAllByUserIdOptimized(User user, OrderDateRequestDto dateRequestDto, int page, int size) {
        log.info("🚀 최적화된 주문 목록 조회 시작 - 사용자 ID: {}, 페이지: {}, 크기: {}", user.getUserId(), page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        // 1단계: 페이징된 주문 ID들만 조회
        Page<Long> orderIds = ordersRepository.findOrderIdsByUserIdAndDate(
            user.getUserId(),
            dateRequestDto.getStartDate() != null ? dateRequestDto.getStartDate().atStartOfDay() : null,
            dateRequestDto.getEndDate() != null ? dateRequestDto.getEndDate().atTime(23, 59, 59) : null,
            pageable
        );
        
        if (orderIds.isEmpty()) {
            log.info("📭 조회된 주문이 없음");
            return Page.empty();
        }
        
        // 2단계: 해당 ID들의 모든 데이터를 Fetch Join으로 한 번에 조회
        List<Orders> orders = ordersRepository.findByIdsWithAllData(orderIds.getContent());
        
        // 주문 ID 순서에 맞게 정렬 (페이징 순서 유지)
        Map<Long, Orders> orderMap = orders.stream()
                .collect(Collectors.toMap(Orders::getId, Function.identity()));
        
        List<OrdersResponseDto> orderedDtos = orderIds.getContent().stream()
                .map(orderMap::get)
                .filter(Objects::nonNull)
                .map(OrdersResponseDto::toDto)
                .toList();
        
        log.info("✅ 최적화된 주문 목록 조회 완료 - {} 건 조회됨 (2단계 조회)", orderedDtos.size());
        return new PageImpl<>(orderedDtos, pageable, orderIds.getTotalElements());
    }


}
