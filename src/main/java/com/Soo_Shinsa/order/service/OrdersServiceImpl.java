package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.cartitem.model.CartItem;
import com.Soo_Shinsa.cartitem.repository.CartItemRepository;
import com.Soo_Shinsa.constant.OrdersStatus;
import com.Soo_Shinsa.constant.ProductStatus;
import com.Soo_Shinsa.coupon.aop.CouponLock;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponUser;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.Soo_Shinsa.exception.ErrorCode.NOT_FOUND_CART;

@Service
@RequiredArgsConstructor
public class OrdersServiceImpl implements OrdersService {
    private final OrdersRepository ordersRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final CouponUserRepository couponUserRepository;
    private final ProductOptionRepository productOptionRepository;


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

        ProductOption productOption = productOptionRepository.findByIdOrElseThrow(productOptionId);
        Product product = productOption.getProduct();
        if (product.getProductStatus().equals(ProductStatus.SOLD_OUT) || product.getProductStatus().equals(ProductStatus.UNAVAILABLE)) {
            throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        if (productOption.getQuantity() < quantity) {
            throw new InvalidInputException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        productOption.decreaseQuantity(quantity);

        BigDecimal totalPrice = productOption.getProduct().getPrice().multiply(BigDecimal.valueOf(quantity));


        Orders order = Orders.builder()
                .user(user)
                .totalPrice(totalPrice)
                .status(OrdersStatus.BEFOREPAYMENT)
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
    @Transactional
    @Override
    public OrdersResponseDto createSingleOrderCartItem (User user, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findByIdOrElseThrow(cartItemId);
        ProductOption productOption = cartItem.getProductOption();
        Product product = cartItem.getProductOption().getProduct();

        if (product.getProductStatus().equals(ProductStatus.SOLD_OUT) || product.getProductStatus().equals(ProductStatus.UNAVAILABLE)) {
            throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        Integer quantity = cartItem.getQuantity();

        if (productOption.getQuantity() < quantity) {
            throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        productOption.decreaseQuantity(quantity);

        isUsedCoupon(user, cartItem);

        Orders order = Orders.builder()
                .user(user)
                .status(OrdersStatus.BEFOREPAYMENT)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .price(product.getPrice())
                .discountPrice(cartItem.getDiscountedPrice())
                .product(product)
                .quantity(cartItem.getQuantity())
                .build();

        order.addOrderItem(orderItem);
        ordersRepository.save(order);

        cartItemRepository.delete(cartItem);

        return OrdersResponseDto.toDto(order);
    }

    @CouponLock(key = "'lock:coupon :' + #user.userId")
    @Transactional
    @Override
    public OrdersResponseDto createAllOrderFromCart(User user) {

        List<CartItem> cartItems = cartItemRepository.findByUserUserId(user.getUserId());
        if (cartItems.isEmpty()) {
            throw new NotFoundException(NOT_FOUND_CART);
        }
        Orders order = Orders.builder()
                .user(user)
                .status(OrdersStatus.BEFOREPAYMENT)
                .build();


        // CartItem 데이터를 기반으로 OrderItem 생성 및 추가
        for (CartItem cartItem : cartItems) {
            ProductOption productOption = cartItem.getProductOption();
            Integer quantity = cartItem.getQuantity();

            if (productOption.getQuantity() < quantity) {
                throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
            }

            isUsedCoupon(user, cartItem);

            BigDecimal discountedPrice = cartItem.getDiscountedPrice() != null ?
                    cartItem.getDiscountedPrice() : productOption.getProduct().getPrice();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .price(productOption.getProduct().getPrice())
                    .discountPrice(discountedPrice)
                    .product(productOption.getProduct())
                    .productOption(productOption)
                    .quantity(quantity)
                    .build();

            order.addOrderItem(orderItem);
        }

        ordersRepository.save(order);

        // 카트 비우기
        cartItemRepository.deleteAll(cartItems);

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
            if (coupon.getMaxCount() <= 0) {
                throw new InvalidInputException(ErrorCode.COUPON_OUT_OF_STOCK);
            }

            coupon.decreaseMaxCount(1);
            CouponUser couponUser = couponUserRepository.findByCouponIdAndUserUserId(coupon.getId(), user.getUserId())
                    .orElseThrow(() -> new InvalidInputException(ErrorCode.NOT_FOUND_COUPON));

            couponUser.markAsUsed();
            couponUserRepository.save(couponUser);
        }
    }
}
