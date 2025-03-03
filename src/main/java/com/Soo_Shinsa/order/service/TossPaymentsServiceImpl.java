package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.global.constant.OrdersStatus;
import com.Soo_Shinsa.global.constant.TossPayMethod;
import com.Soo_Shinsa.global.constant.TossPayStatus;
import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.Soo_Shinsa.order.dto.PayloadRequestDto;
import com.Soo_Shinsa.order.dto.PaymentRequestDto;
import com.Soo_Shinsa.order.dto.PaymentResponseDto;
import com.Soo_Shinsa.order.dto.UserOrderDto;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.order.model.Payment;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import com.Soo_Shinsa.order.repository.PaymentRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

import static com.Soo_Shinsa.global.constant.OrdersStatus.ORDERCOMPLETED;


@Service
@RequiredArgsConstructor
public class TossPaymentsServiceImpl implements TossPaymentsService {
    private final PaymentRepository paymentRepository;
    private final OrdersRepository ordersRepository;
    private final UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${toss.secret-key}")
    private String secretKey;


    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto requestDto, User user) {

        Orders order = ordersRepository.findByOrderId(requestDto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("오더가 없습니다"));

        order.updateStatus(OrdersStatus.ORDERCOMPLETED);

        Payment payment = new Payment(
                order.getOrderId(),
                order.getTotalPrice(),
                TossPayStatus.PENDING,
                TossPayMethod.CARD,
                order,
                user
        );

        Payment savedPayment = paymentRepository.save(payment);
        return PaymentResponseDto.toDto(savedPayment);
    }


    @Transactional
    public void approvePayment(String paymentKey, String orderId, Long amount, Model model) throws JsonProcessingException {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes()));
        headers.setContentType(MediaType.APPLICATION_JSON);

        Payment findPayment = paymentRepository.findByOrderId(orderId);
        findPayment.update(TossPayStatus.PAYMENT, paymentKey);
        paymentRepository.save(findPayment);

        PayloadRequestDto payload = new PayloadRequestDto(orderId, String.valueOf(amount));
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
        restTemplate.postForEntity("https://api.tosspayments.com/v1/payments/" + paymentKey, request, JsonNode.class);
        Orders findOrder = ordersRepository.findByOrderId(orderId)
                .orElseThrow(() -> new InvalidInputException(ErrorCode.NOT_FOUND_ORDER));
        findOrder.updateStatus(ORDERCOMPLETED);
    }

    @Transactional
    @Override
    public void cancelPayment(String paymentKey, String cancelReason) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes()));
        headers.setContentType(MediaType.APPLICATION_JSON);

        Payment findPayment = paymentRepository.findByPaymentKey(paymentKey);

        String orderId = findPayment.getOrderId();
        Orders findOrder = ordersRepository.findByOrderId(orderId)
                .orElseThrow(() -> new InvalidInputException(ErrorCode.NOT_FOUND_ORDER));
        findOrder.updateStatus(OrdersStatus.ORDERCANCEL);
        ordersRepository.save(findOrder);

        findPayment.update(TossPayStatus.CANCEL, paymentKey);
        paymentRepository.save(findPayment);

        PayloadRequestDto payload = new PayloadRequestDto(cancelReason);
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

      restTemplate.postForEntity("https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel", request, JsonNode.class);
    }


    @Transactional
    public UserOrderDto findItem(Long userId, Long orderId) {
        User user = userRepository.findByIdOrElseThrow(userId);
        Orders order = ordersRepository.findByIdOrElseThrow(orderId);
        return new UserOrderDto(user, order);
    }
}