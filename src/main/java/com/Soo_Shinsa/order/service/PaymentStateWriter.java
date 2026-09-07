package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.global.constant.OrdersStatus;
import com.Soo_Shinsa.global.constant.TossPayStatus;
import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.Soo_Shinsa.global.exception.NoAuthorizedException;
import com.Soo_Shinsa.global.utils.EntityValidator;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.order.model.Payment;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import com.Soo_Shinsa.order.repository.PaymentRepository;
import com.Soo_Shinsa.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 결제 관련 DB 변경.
 *
 * 토스 호출을 트랜잭션 안에서 하면, 토스가 느려질 때 DB 커넥션을 그만큼 붙잡는다.
 * 커넥션 풀이 10개라 몇 건만 지연돼도 앱 전체가 멎는다.
 * 그래서 DB 작업을 이 클래스로 떼어내고, 외부 호출은 트랜잭션 밖에서 한다.
 */
@Component
@RequiredArgsConstructor
public class PaymentStateWriter {

    private final PaymentRepository paymentRepository;
    private final OrdersRepository ordersRepository;
    private final OrderCancellationService orderCancellationService;

    /**
     * 주문 취소 전 검증. 결제되어 있으면 paymentKey 를, 아니면 null 을 돌려준다.
     * lazy 연관(order.user)을 건드리므로 트랜잭션 안에서 해야 한다.
     */
    @Transactional(readOnly = true)
    public String verifyOrderCancellable(Long orderId, User requester) {
        Orders order = ordersRepository.findByIdOrElseThrow(orderId);
        EntityValidator.validateAndOrders(order, requester.getUserId());

        if (order.getStatus() == OrdersStatus.ORDERCANCEL) {
            throw new InvalidInputException(ErrorCode.ALREADY_CANCEL_ORDER);
        }

        Payment payment = paymentRepository.findByOrderId(order.getOrderId());
        return payment != null && payment.getStatus() == TossPayStatus.PAYMENT
                ? payment.getPaymentKey()
                : null;
    }

    /** 취소 전 검증. 결제 금액을 함께 돌려준다. */
    @Transactional(readOnly = true)
    public Long verifyCancellable(String paymentKey, User requester) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey);
        if (payment == null) {
            throw new InvalidInputException(ErrorCode.NOT_FOUND_ORDER);
        }
        Orders order = ordersRepository.findByOrderId(payment.getOrderId())
                .orElseThrow(() -> new InvalidInputException(ErrorCode.NOT_FOUND_ORDER));

        // paymentKey 만 알면 남의 결제도 취소할 수 있었다
        EntityValidator.validateAndOrders(order, requester.getUserId());
        return order.getId();
    }

    /** 토스 취소가 끝난 뒤 반영한다. */
    @Transactional
    public void applyCancellation(String paymentKey, Long orderId, String cancelReason) {
        orderCancellationService.cancel(orderId, cancelReason);

        Payment payment = paymentRepository.findByPaymentKey(paymentKey);
        payment.update(TossPayStatus.CANCEL, paymentKey);
        paymentRepository.save(payment);
    }

    /** 승인 전 검증. 요청 금액이 주문 금액과 같은지 확인한다. */
    @Transactional(readOnly = true)
    public void verifyApprovable(String orderId, Long amount) {
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            throw new InvalidInputException(ErrorCode.NOT_FOUND_ORDER);
        }
        Orders order = ordersRepository.findByOrderId(orderId)
                .orElseThrow(() -> new InvalidInputException(ErrorCode.NOT_FOUND_ORDER));

        // 클라이언트가 넘긴 금액을 그대로 승인 요청에 싣고 있었다
        if (order.getTotalPrice().compareTo(BigDecimal.valueOf(amount)) != 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }
    }

    /** 토스 승인이 끝난 뒤 반영한다. */
    @Transactional
    public void applyApproval(String orderId, String paymentKey) {
        Payment payment = paymentRepository.findByOrderId(orderId);
        payment.update(TossPayStatus.PAYMENT, paymentKey);
        paymentRepository.save(payment);

        Orders order = ordersRepository.findByOrderId(orderId)
                .orElseThrow(() -> new InvalidInputException(ErrorCode.NOT_FOUND_ORDER));
        order.updateStatus(OrdersStatus.ORDERCOMPLETED);
        ordersRepository.save(order);
    }
}
