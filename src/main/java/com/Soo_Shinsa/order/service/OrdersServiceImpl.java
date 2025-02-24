package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.cartitem.model.CartItem;
import com.Soo_Shinsa.cartitem.repository.CartItemRepository;
import com.Soo_Shinsa.constant.OrdersStatus;
import com.Soo_Shinsa.constant.ProductStatus;
import com.Soo_Shinsa.coupon.aop.CouponLock;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponUser;
import com.Soo_Shinsa.coupon.repository.CouponRepository;
import com.Soo_Shinsa.coupon.repository.CouponUserRepository;
import com.Soo_Shinsa.exception.ErrorCode;
import com.Soo_Shinsa.exception.InternalServerException;
import com.Soo_Shinsa.exception.InvalidInputException;
import com.Soo_Shinsa.exception.NotFoundException;
import com.Soo_Shinsa.order.dto.OrderDateRequestDto;
import com.Soo_Shinsa.order.dto.OrdersResponseDto;
import com.Soo_Shinsa.order.model.OrderItem;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import com.Soo_Shinsa.product.aop.StockLock;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import com.Soo_Shinsa.product.repository.ProductOptionRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import com.Soo_Shinsa.utils.EntityValidator;
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
        productOptionRepository.saveAndFlush(productOption);
        log.info("📉 재고 감소 완료 - 남은 재고: {}", productOption.getQuantity());

        BigDecimal totalPrice = productOption.getProduct().getPrice().multiply(BigDecimal.valueOf(quantity));


        Orders order = Orders.builder()
                .user(user)
                .totalPrice(totalPrice)
                .status(OrdersStatus.ORDERCOMPLETED)
                .build();


        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .productOption(productOption)
                .price(productOption.getProduct().getPrice())
                .quantity(quantity)
                .build();

        order.addOrderItem(orderItem);

        ordersRepository.save(order);

        return OrdersResponseDto.toDto(order);
    }

    @StockLock(key = "'lock:productOption:' + #productOption.id")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public OrdersResponseDto createSingleOrderCartItem(User user, Long cartItemId) {


        CartItem cartItem = cartItemRepository.findByIdOrElseThrow(cartItemId);
        ProductOption productOption = productOptionRepository.findByIdWithLock(cartItem.getProductOption().getId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_PRODUCT_OPTION));

        log.info("🔒 StockLock 적용 확인 - productOptionId: {}", productOption.getId());
        Product product = cartItem.getProductOption().getProduct();

        if (product.getProductStatus().equals(ProductStatus.SOLD_OUT) || product.getProductStatus().equals(ProductStatus.UNAVAILABLE)) {
            throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        if (productOption.getQuantity() < cartItem.getQuantity()) {
            throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        productOption.decreaseQuantity(cartItem.getQuantity());
        productOptionRepository.saveAndFlush(productOption);

        isUsedCoupon(user, cartItem);

        Orders order = Orders.builder()
                .user(user)
                .totalPrice(cartItem.getDiscountedPrice() != null
                        ? cartItem.getDiscountedPrice()
                        : product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .status(OrdersStatus.ORDERCOMPLETED)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .price(product.getPrice())
                .productOption(productOption)
                .discountPrice(cartItem.getDiscountedPrice())
                .product(product)
                .quantity(cartItem.getQuantity())
                .build();

        order.addOrderItem(orderItem);
        ordersRepository.save(order);

        cartItemRepository.delete(cartItem);

        return OrdersResponseDto.toDto(order);
    }

    @CouponLock(key = "'lock:coupon:' + #user.userId")
    @StockLock(key = "'lock:productOption:' + #user.userId")
    @Transactional
    @Override
    public OrdersResponseDto createAllOrderFromCart(User user) {
        List<CartItem> cartItems = cartItemRepository.findByUserUserIdWithLock(user.getUserId());

        if (cartItems.isEmpty()) {
            throw new NotFoundException(ErrorCode.NOT_FOUND_CART);
        }

        Orders order = Orders.builder()
                .user(user)
                .status(OrdersStatus.ORDERCOMPLETED)
                .build();

        for (CartItem cartItem : cartItems) {
            ProductOption productOption = cartItem.getProductOption();
            Integer quantity = cartItem.getQuantity();

            log.info("🔍 주문 처리 시작 - 상품 ID: {}, 현재 재고: {}", productOption.getId(), productOption.getQuantity());

            // ✅ 쿠폰 적용을 먼저 수행하여 할인 가격이 올바르게 반영되도록 함
            isUsedCoupon(user, cartItem);

            // ✅ 재고 확인 후 감소
            if (productOption.getQuantity() < quantity) {
                log.error("❌ 주문 실패 - 재고 부족 (상품 ID: {})", productOption.getId());

                if (cartItem.getCoupon() != null) {
                    log.warn("🚨 재고 부족으로 인해 쿠폰이 사용되지 않음 - 쿠폰 ID: {}", cartItem.getCoupon().getId());
                }
                continue;
            }

            // ✅ 재고 감소
            productOption.decreaseQuantity(quantity);
            productOptionRepository.saveAndFlush(productOption);

            // ✅ 할인된 가격이 있으면 할인된 가격 적용, 없으면 원래 가격 적용
            BigDecimal discountPrice = cartItem.getDiscountedPrice() != null
                    ? cartItem.getDiscountedPrice()
                    : productOption.getProduct().getPrice().multiply(BigDecimal.valueOf(quantity));

            // ✅ 주문 아이템 생성
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .price(productOption.getProduct().getPrice())
                    .discountPrice(discountPrice)
                    .product(productOption.getProduct())
                    .productOption(productOption)
                    .quantity(quantity)
                    .build();

            order.addOrderItem(orderItem);
            log.info("✅ 주문 아이템 생성 완료 - 상품 ID: {}, 남은 재고: {}", productOption.getId(), productOption.getQuantity());
        }

        cartItemRepository.deleteAllInBatch(cartItems);
        ordersRepository.saveAndFlush(order);

        log.info("✅ 전체 주문 완료 - 최종 재고 확인");
        return OrdersResponseDto.toDto(order);
    }

    @Transactional
    @Override
    public OrdersResponseDto createOrder(User user) {

        Orders order = Orders.builder()
                .totalPrice(BigDecimal.ZERO)
                .user(user)
                .status(OrdersStatus.BEFOREPAYMENT)
                .build();

        Orders savedOrder = ordersRepository.save(order);


        return OrdersResponseDto.toDto(savedOrder);
    }

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

    private void isUsedCoupon(User user, CartItem cartItem) {
        Coupon coupon = cartItem.getCoupon();
        if (coupon != null) {
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
        }
    }
}
