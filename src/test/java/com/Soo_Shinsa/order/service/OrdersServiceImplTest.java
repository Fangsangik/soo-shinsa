package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.cartitem.model.CartItem;
import com.Soo_Shinsa.cartitem.repository.CartItemRepository;
import com.Soo_Shinsa.constant.OrdersStatus;
import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.exception.InvalidInputException;
import com.Soo_Shinsa.exception.NoAuthorizedException;
import com.Soo_Shinsa.exception.NotFoundException;
import com.Soo_Shinsa.order.dto.OrderDateRequestDto;
import com.Soo_Shinsa.order.dto.OrdersResponseDto;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.Soo_Shinsa.constant.OrdersStatus.*;
import static com.Soo_Shinsa.support.Fixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdersServiceImplTest {

    @Mock ProductRepository productRepository;
    @Mock OrdersRepository ordersRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock UserRepository userRepository;
    @Mock com.Soo_Shinsa.user.service.PointService pointService;
    @InjectMocks OrdersServiceImpl service;

    User customer, otherCustomer, vendor, admin;
    Product product;

    @BeforeEach
    void setUp() {
        customer = user(1L, Role.CUSTOMER);
        otherCustomer = user(2L, Role.CUSTOMER);
        vendor = user(3L, Role.VENDOR);
        admin = user(4L, Role.ADMIN);
        product = product(10L, brand(100L, vendor), 10_000);
        lenient().when(userRepository.findByIdOrElseThrow(anyLong())).thenAnswer(i -> {
            long id = i.getArgument(0);
            return List.of(customer, otherCustomer, vendor, admin).stream().filter(u -> u.getUserId() == id).findFirst().orElseThrow();
        });
        lenient().when(ordersRepository.save(any(Orders.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Nested
    class 고객_상태_전이_규칙 {
        @ParameterizedTest(name = "{0} → {1} = {2}")
        @CsvSource({
                "BEFOREPAYMENT, CANCEL, true", "PAYMENTCOMPLETED, CANCEL, true", "ONDELIVERY, CANCEL, false",
                "DELIVERYCOMPLETED, REQUESTFORRETURN, true", "ORDERCOMPLETED, APPLYFOREXCHANGE, true", "PAYMENTCOMPLETED, REQUESTFORRETURN, false",
                "REQUESTFORRETURN, CANCELRETURN, true", "APPLYFOREXCHANGE, EXCHANGECANCELLATION, true",
                "BEFOREPAYMENT, PAYMENTCOMPLETED, false", "PAYMENTCOMPLETED, ONDELIVERY, false", "ONDELIVERY, DELIVERYCOMPLETED, false",
                "CANCEL, RETURN, false", "REQUESTFORRETURN, RETURNCOMPLETED, false"
        })
        void 전이표(OrdersStatus from, OrdersStatus to, boolean allowed) {
            assertThat(OrdersServiceImpl.isCustomerTransitionAllowed(from, to)).isEqualTo(allowed);
        }

        @Test
        void 고객은_자기_주문만_바꿀_수_있다() {
            Orders order = order(1L, otherCustomer, BEFOREPAYMENT);
            when(ordersRepository.findByIdOrElseThrow(1L)).thenReturn(order);
            assertThatThrownBy(() -> service.updateOrder(customer, 1L, CANCEL)).isInstanceOf(NoAuthorizedException.class);
            assertThat(order.getStatus()).isEqualTo(BEFOREPAYMENT);
        }

        @Test
        void 고객은_결제완료로_직접_바꿀_수_없다() {
            Orders order = order(1L, customer, BEFOREPAYMENT);
            when(ordersRepository.findByIdOrElseThrow(1L)).thenReturn(order);
            assertThatThrownBy(() -> service.updateOrder(customer, 1L, PAYMENTCOMPLETED)).isInstanceOf(InvalidInputException.class);
        }

        @Test
        void 고객_취소는_허용된다() {
            Orders order = order(1L, customer, BEFOREPAYMENT);
            when(ordersRepository.findByIdOrElseThrow(1L)).thenReturn(order);
            assertThat(service.updateOrder(customer, 1L, CANCEL).getStatus()).isEqualTo(CANCEL);
        }
    }

    @Nested
    class 관리자_입점사_상태_변경 {
        @Test
        void 관리자는_어떤_상태로든_바꿀_수_있다() {
            Orders order = order(1L, customer, BEFOREPAYMENT);
            when(ordersRepository.findByIdOrElseThrow(1L)).thenReturn(order);
            assertThat(service.updateOrder(admin, 1L, ONDELIVERY).getStatus()).isEqualTo(ONDELIVERY);
            verifyNoInteractions(cartItemRepository);
        }

        @Test
        void 입점사는_자기_브랜드_상품이_없는_주문을_바꿀_수_없다() {
            Orders order = order(1L, customer, PAYMENTCOMPLETED);
            when(ordersRepository.findByIdOrElseThrow(1L)).thenReturn(order);
            when(ordersRepository.existsByIdAndVendorUserId(1L, vendor.getUserId())).thenReturn(false);
            assertThatThrownBy(() -> service.updateOrder(vendor, 1L, ONDELIVERY)).isInstanceOf(NoAuthorizedException.class);
        }

        @Test
        void 입점사는_자기_브랜드_상품이_포함된_주문을_배송_처리한다() {
            Orders order = order(1L, customer, PAYMENTCOMPLETED);
            when(ordersRepository.findByIdOrElseThrow(1L)).thenReturn(order);
            when(ordersRepository.existsByIdAndVendorUserId(1L, vendor.getUserId())).thenReturn(true);
            assertThat(service.updateOrder(vendor, 1L, ONDELIVERY).getStatus()).isEqualTo(ONDELIVERY);
        }
    }

    @Nested
    class 장바구니_주문 {
        @Test
        void 빈_장바구니면_NotFound() {
            when(cartItemRepository.findByUserUserId(customer.getUserId())).thenReturn(List.of());
            assertThatThrownBy(() -> service.createAllOrderFromCart(customer)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void 할인가_x_수량의_합이_총액이고_장바구니는_비워진다() {
            CartItem c1 = cartItem(1L, customer, product, option(1L, product), 2);           // 10,000 × 2
            CartItem c2 = cartItem(2L, customer, product, option(2L, product), 1);
            c2.applyCoupon(coupon(1L, 10, com.Soo_Shinsa.constant.CouponType.UNIVERSAL, 10), new BigDecimal("9000")); // 9,000 × 1
            when(cartItemRepository.findByUserUserId(customer.getUserId())).thenReturn(List.of(c1, c2));

            OrdersResponseDto dto = service.createAllOrderFromCart(customer);

            assertThat(dto.getTotalPrice()).isEqualByComparingTo("29000");
            assertThat(dto.getStatus()).isEqualTo(BEFOREPAYMENT);
            assertThat(dto.getOrderItems()).hasSize(2);
            assertThat(dto.getOrderItems().get(1).getDiscountPrice()).isEqualByComparingTo("9000");
            verify(cartItemRepository).deleteAll(List.of(c1, c2));
        }

        @Test
        void 단일_장바구니_주문은_남의_카트를_거부하고_해당_항목만_지운다() {
            CartItem mine = cartItem(1L, customer, product, option(1L, product), 1);
            CartItem theirs = cartItem(2L, otherCustomer, product, option(1L, product), 1);
            when(cartItemRepository.findByIdOrElseThrow(2L)).thenReturn(theirs);
            assertThatThrownBy(() -> service.createSingleOrderCartItem(customer, 2L)).isInstanceOf(NoAuthorizedException.class);

            when(cartItemRepository.findByIdOrElseThrow(1L)).thenReturn(mine);
            service.createSingleOrderCartItem(customer, 1L);
            verify(cartItemRepository).delete(mine);
            verify(cartItemRepository, never()).deleteAll();
        }
    }

    @Nested
    class 포인트 {
        @Test
        void 장바구니_주문에_포인트를_쓰면_영속_사용자_잔액을_차감하고_결제_금액에서_뺀다() {
            CartItem c1 = cartItem(1L, customer, product, option(1L, product), 2); // 20,000
            when(cartItemRepository.findByUserUserId(customer.getUserId())).thenReturn(List.of(c1));
            when(pointService.use(eq(customer), eq(new BigDecimal("3000")), any())).thenReturn(new BigDecimal("3000"));

            OrdersResponseDto dto = service.createAllOrderFromCart(customer, new BigDecimal("3000"));

            assertThat(dto.getTotalPrice()).isEqualByComparingTo("17000");
            assertThat(dto.getUsedPoint()).isEqualByComparingTo("3000");
            verify(pointService).use(eq(customer), eq(new BigDecimal("3000")), eq(new BigDecimal("20000")));
        }

        @Test
        void 포인트를_안_쓰면_PointService_를_부르지_않는다() {
            when(cartItemRepository.findByUserUserId(customer.getUserId())).thenReturn(List.of(cartItem(1L, customer, product, option(1L, product), 1)));
            service.createAllOrderFromCart(customer, null);
            service.createAllOrderFromCart(customer, BigDecimal.ZERO);
            verify(pointService, never()).use(any(), any(), any());
        }

        @Test
        void 상태_변경마다_PointService_에_전이를_알린다() {
            Orders order = order(1L, customer, BEFOREPAYMENT);
            when(ordersRepository.findByIdOrElseThrow(1L)).thenReturn(order);
            service.updateOrder(admin, 1L, PAYMENTCOMPLETED);
            service.updateOrder(customer, 1L, CANCEL);
            verify(pointService).onStatusChanged(order, PAYMENTCOMPLETED);
            verify(pointService).onStatusChanged(order, CANCEL);
        }
    }

    @Nested
    class 관리_목록 {
        @SuppressWarnings("unchecked")
        @Test
        void 관리자는_전체_입점사는_자기_것만() {
            Page<OrdersResponseDto> empty = Page.empty();
            when(ordersRepository.getAllForStaff(any(), any(), any(), any(Pageable.class))).thenReturn(empty);
            OrderDateRequestDto d = new OrderDateRequestDto();

            service.getAllForStaff(admin, null, d, 0, 10);
            service.getAllForStaff(vendor, CANCEL, d, 1, 5);

            ArgumentCaptor<Long> vendorId = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(ordersRepository, times(2)).getAllForStaff(vendorId.capture(), any(), eq(d), pageable.capture());
            assertThat(vendorId.getAllValues()).containsExactly(null, vendor.getUserId());
            assertThat(pageable.getAllValues().get(1)).isEqualTo(PageRequest.of(1, 5));
        }

        @Test
        void 고객은_관리_목록을_볼_수_없다() {
            assertThatThrownBy(() -> service.getAllForStaff(customer, null, new OrderDateRequestDto(), 0, 10)).isInstanceOf(NoAuthorizedException.class);
            verifyNoInteractions(ordersRepository);
        }
    }
}
