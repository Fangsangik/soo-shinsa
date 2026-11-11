package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.constant.OrdersStatus;
import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.constant.TossPayMethod;
import com.Soo_Shinsa.constant.TossPayStatus;
import com.Soo_Shinsa.exception.NoAuthorizedException;
import com.Soo_Shinsa.exception.NotFoundException;
import com.Soo_Shinsa.order.dto.PaymentRequestDto;
import com.Soo_Shinsa.order.dto.PaymentResponseDto;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.order.model.Payment;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import com.Soo_Shinsa.order.repository.PaymentRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static com.Soo_Shinsa.support.Fixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TossPaymentsServiceImplTest {
    @Mock PaymentRepository paymentRepository;
    @Mock OrdersRepository ordersRepository;
    @Mock UserRepository userRepository;
    @InjectMocks TossPaymentsServiceImpl service;

    User me, other;
    Orders order;

    @BeforeEach
    void setUp() {
        me = user(1L, Role.CUSTOMER);
        other = user(2L, Role.CUSTOMER);
        order = order(10L, me, OrdersStatus.BEFOREPAYMENT);
        ReflectionTestUtils.setField(order, "totalPrice", new BigDecimal("29000"));
        ReflectionTestUtils.setField(service, "secretKey", "test_sk");
        lenient().when(ordersRepository.findById(10L)).thenReturn(Optional.of(order));
    }

    @Test
    void 남의_주문으로는_결제를_만들_수_없다() {
        assertThatThrownBy(() -> service.createPayment(new PaymentRequestDto(null, TossPayMethod.CARD, 10L), other)).isInstanceOf(NoAuthorizedException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void 결제_생성은_주문_금액으로_READY_상태_Payment_를_만든다() {
        when(paymentRepository.findByOrderId(order.getOrderId())).thenReturn(null);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponseDto dto = service.createPayment(new PaymentRequestDto(null, TossPayMethod.CARD, 10L), me);

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("29000");
        assertThat(saved.getValue().getStatus()).isEqualTo(TossPayStatus.READY);
        assertThat(saved.getValue().isAmountValid()).isTrue();
        assertThat(dto.getOrderId()).isEqualTo(order.getOrderId());
    }

    @Test
    void 결제_페이지_재진입은_기존_Payment_를_재사용한다() {
        Payment existing = new Payment(order.getOrderId(), new BigDecimal("29000"), TossPayStatus.READY, TossPayMethod.CARD, order, me);
        when(paymentRepository.findByOrderId(order.getOrderId())).thenReturn(existing);

        service.createPayment(new PaymentRequestDto(null, TossPayMethod.CARD, 10L), me);
        service.createPayment(new PaymentRequestDto(null, TossPayMethod.CARD, 10L), me);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void 승인_콜백에_Payment_가_없으면_NotFound_이고_Toss_호출_전에_멈춘다() {
        when(paymentRepository.findByOrderId("ORD-x")).thenReturn(null);
        assertThatThrownBy(() -> service.approvePayment("pk", "ORD-x", 29000L, null)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(ordersRepository);
    }
}
