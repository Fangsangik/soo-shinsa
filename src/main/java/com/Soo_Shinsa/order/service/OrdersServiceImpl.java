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
import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.InternalServerException;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.Soo_Shinsa.global.exception.NotFoundException;
import com.Soo_Shinsa.global.utils.EntityValidator;
import com.Soo_Shinsa.order.dto.OrderCreateRequestDto;
import com.Soo_Shinsa.order.dto.OrderDateRequestDto;
import com.Soo_Shinsa.order.dto.OrdersResponseDto;
import com.Soo_Shinsa.order.model.OrderItem;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.order.model.Payment;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import com.Soo_Shinsa.order.repository.PaymentRepository;
import com.Soo_Shinsa.product.aop.StockLock;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import com.Soo_Shinsa.product.repository.ProductOptionRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdersServiceImpl implements OrdersService {
    private final OrdersRepository ordersRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final CouponUserRepository couponUserRepository;
    private final ProductOptionRepository productOptionRepository;
    private final CouponRepository couponRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentsService tossPaymentsService;
    private final CartItemProductOptionRepository cartItemProductOptionRepository;


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
    @StockLock(key = "'lock:productOption:' + #productOption.Id")
    @Transactional
    @Override
    public OrdersResponseDto createSingleProductOrder(User user, Long productOptionId, Integer quantity) {

        log.info("🔒 StockLock 적용 확인 - productOptionId: {}", productOptionId);

        ProductOption productOption = productOptionRepository.findByIdOrElseThrow(productOptionId);
        log.info("🛒 현재 재고: {}", productOption.getQuantity());

        Product product = productOption.getProduct();
        if (product.getProductStatus().equals(ProductStatus.SOLD_OUT) || product.getProductStatus().equals(ProductStatus.UNAVAILABLE)) {
            throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        if (productOption.getQuantity() < quantity) {
            throw new InvalidInputException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        productOption.decreaseQuantity(quantity);
        productOption.increaseSaleCount(quantity);
        productOptionRepository.saveAndFlush(productOption);
        log.info("📉 재고 감소 완료 - 남은 재고: {}", productOption.getQuantity());

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

    @StockLock(key = "'lock:productOption:' + #productOption.Id")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public OrdersResponseDto createSingleOrderCartItem(User user, OrderCreateRequestDto requestDto) {
        CartItem cartItem = cartItemRepository.findByIdOrElseThrow(requestDto.getCartId());

        List<ProductOption> productOptions = cartItem.getProductOptions().stream()
                .map(cartItemProductOption -> productOptionRepository.findByIdOrElseThrow(cartItemProductOption.getProductOption().getId()))
                .toList();

        Product product = cartItem.getProduct();

        if (product.getProductStatus().equals(ProductStatus.SOLD_OUT) || product.getProductStatus().equals(ProductStatus.UNAVAILABLE)) {
            throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        for (ProductOption option : productOptions) {
            if (option.getQuantity() < cartItem.getQuantity()) {
                throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
            }
            option.increaseSaleCount(cartItem.getQuantity());
            option.decreaseQuantity(cartItem.getQuantity());
            productOptionRepository.saveAndFlush(option);
        }

        try {
            isUsedCoupon(user, cartItem);
        } catch (Exception e) {
            log.warn("⚠️ 쿠폰 적용 중 오류 발생. 카트 아이템 ID: {}", cartItem.getId());
        }

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


    @StockLock(key = "'lock:productOption:' + #productOption.Id")
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
                    .toList();

            Integer quantity = cartItem.getQuantity();

            BigDecimal cartItemTotalPrice = BigDecimal.ZERO; // **각 CartItem별 개별 가격 계산**

            for (ProductOption option : productOptions) {
                int updatedRows = productOptionRepository.decreaseStock(option.getId(), quantity);
                if (updatedRows == 0) {
                    log.error("🚨 재고 부족으로 주문 실패 - 상품 옵션 ID: {}, 요청 수량: {}", option.getId(), quantity);
                    throw new InvalidInputException(ErrorCode.CAN_NOT_USE_PRODUCT);
                }
                option.increaseSaleCount(quantity);
                productOptionRepository.save(option);
            }

            try {
                log.info("🎟 쿠폰 적용 시도 - 카트 아이템 ID: {}", cartItem.getId());
                isUsedCoupon(user, cartItem);
            } catch (Exception e) {
                log.warn("⚠️ 쿠폰 적용 중 오류 발생. 카트 아이템 ID: {}", cartItem.getId());
            }

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

        Coupon coupon = couponRepository.findByIdOrElseThrow(cartItem.getCoupon().getId());

        //Coupon coupon = couponRepository.findByIdWithLock(cartItem.getCoupon().getId())
        //        .orElseThrow(() -> new InvalidInputException(ErrorCode.NOT_FOUND_COUPON));

        log.info("🎟 쿠폰 사용 검증 시작 - 쿠폰 ID: {}, 현재 재고: {}", coupon.getId(), coupon.getMaxCount());

        if (coupon.getMaxCount() <= 0) {
            log.error("🚨 쿠폰 사용 불가 - 쿠폰 ID: {}, 재고 부족", coupon.getId());
            throw new InvalidInputException(ErrorCode.COUPON_OUT_OF_STOCK);
        }

        CouponUser couponUser = couponUserRepository.findByCouponIdAndUserUserId(coupon.getId(), user.getUserId())
                .orElseThrow(() -> new InvalidInputException(ErrorCode.NOT_FOUND_COUPON));

        if (couponUser.isUsed()) {
            log.error("🚨 이미 사용된 쿠폰 - 쿠폰 ID: {}", coupon.getId());
            throw new InvalidInputException(ErrorCode.ALREADY_USED_COUPON);
        }

        // ✅ 즉시 반영하도록 트랜잭션 분리
        coupon.decreaseMaxCount(1);
        couponUser.markAsUsed();
        couponRepository.saveAndFlush(coupon);
        couponUserRepository.saveAndFlush(couponUser);
        log.info("✅ 쿠폰 사용 완료 - 쿠폰 ID: {}, 남은 재고: {}", coupon.getId(), coupon.getMaxCount());

        // 쿠폰 적용 후 카트 아이템 업데이트
        cartItem.applyCoupon(coupon);
        cartItemRepository.saveAndFlush(cartItem);
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

        // 결제 정보 조회
        Payment payment = paymentRepository.findByOrderId(findOrder.getOrderId());

        if (payment != null && payment.getStatus() == TossPayStatus.PAYMENT) {
            // 결제 완료 상태라면 결제 취소 실행
            tossPaymentsService.cancelPayment(payment.getPaymentKey(), "사용자 주문 취소");
        }

        // 주문 상태 변경
        findOrder.updateStatus(OrdersStatus.ORDERCANCEL);
        ordersRepository.save(findOrder);
    }


}
