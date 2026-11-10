package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.cartitem.model.CartItem;
import com.Soo_Shinsa.cartitem.repository.CartItemRepository;
import com.Soo_Shinsa.constant.OrdersStatus;
import com.Soo_Shinsa.constant.ProductStatus;
import com.Soo_Shinsa.exception.ErrorCode;
import com.Soo_Shinsa.exception.InternalServerException;
import com.Soo_Shinsa.exception.NotFoundException;
import com.Soo_Shinsa.order.dto.OrderDateRequestDto;
import com.Soo_Shinsa.order.dto.OrdersResponseDto;
import com.Soo_Shinsa.order.model.OrderItem;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.repository.ProductRepository;
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
    private final ProductRepository productRepository;
    private final OrdersRepository ordersRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;


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
    @Transactional
    @Override
    public OrdersResponseDto createSingleProductOrder(User user, Long productId, Integer quantity) {

        Product product = productRepository.findByIdOrElseThrow(productId);
        if (product.getProductStatus().equals(ProductStatus.SOLD_OUT) || product.getProductStatus().equals(ProductStatus.UNAVAILABLE)) {
            throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        Orders order = Orders.builder()
                .user(user)
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .status(OrdersStatus.BEFOREPAYMENT)
                .build();


        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(quantity)
                .build();

        order.addOrderItem(orderItem);

        ordersRepository.save(order);

        return OrdersResponseDto.toDto(order);
    }

    @Transactional
    @Override
    public OrdersResponseDto createSingleOrderCartItem (User user, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findByIdOrElseThrow(cartItemId);

        Product product = cartItem.getProductOption().getProduct();

        if (product.getProductStatus().equals(ProductStatus.SOLD_OUT) || product.getProductStatus().equals(ProductStatus.UNAVAILABLE)) {
            throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        // 카트 아이템의 최종 금액 (쿠폰 적용된 금액) 사용
        BigDecimal discountedPrice = cartItem.getDiscountedPrice();
        if (discountedPrice == null || discountedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            discountedPrice = product.getPrice();
        }

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

        cartItemRepository.deleteAll();

        return OrdersResponseDto.toDto(order);
    }

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
            Product product = cartItem.getProductOption().getProduct();
            Integer quantity = cartItem.getQuantity();


            BigDecimal discountedPrice = cartItem.getDiscountedPrice() != null ?
                    cartItem.getDiscountedPrice() : product.getPrice();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .price(product.getPrice())
                    .discountPrice(discountedPrice)
                    .product(product)
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

        if (!findUser.isAdminOrVendor()) {
            EntityValidator.validateAndOrders(findOrder, findUser.getUserId());
            if (!isCustomerTransitionAllowed(findOrder.getStatus(), status)) {
                throw new com.Soo_Shinsa.exception.InvalidInputException(ErrorCode.INVALID_ORDER_STATUS);
            }
        }
        findOrder.updateStatus(status);
        Orders savedOrder = ordersRepository.save(findOrder);
        return OrdersResponseDto.toDto(savedOrder);
    }

    /** 고객이 직접 바꿀 수 있는 상태 전이. 결제/배송 상태는 ADMIN/VENDOR만 변경 가능. */
    static boolean isCustomerTransitionAllowed(OrdersStatus from, OrdersStatus to) {
        return switch (to) {
            case CANCEL -> from == OrdersStatus.BEFOREPAYMENT || from == OrdersStatus.PAYMENTCOMPLETED;
            case REQUESTFORRETURN, APPLYFOREXCHANGE -> from == OrdersStatus.DELIVERYCOMPLETED || from == OrdersStatus.ORDERCOMPLETED;
            case CANCELRETURN -> from == OrdersStatus.REQUESTFORRETURN;
            case EXCHANGECANCELLATION -> from == OrdersStatus.APPLYFOREXCHANGE;
            default -> false;
        };
    }
}
