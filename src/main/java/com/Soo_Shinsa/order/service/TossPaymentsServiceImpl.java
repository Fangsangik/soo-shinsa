package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.global.constant.OrdersStatus;
import com.Soo_Shinsa.global.constant.TossPayMethod;
import com.Soo_Shinsa.global.constant.TossPayStatus;
import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.utils.EntityValidator;
import com.Soo_Shinsa.global.exception.NoAuthorizedException;
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
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Base64;

import static com.Soo_Shinsa.global.constant.OrdersStatus.ORDERCOMPLETED;


@Service
@RequiredArgsConstructor
public class TossPaymentsServiceImpl implements TossPaymentsService {
    private final PaymentRepository paymentRepository;
    private final OrdersRepository ordersRepository;
    private final UserRepository userRepository;
    private final PaymentStateWriter paymentStateWriter;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${toss.secret_api_key}")
    private String secretKey;

    @Value("${toss.base-url}")
    private String tossBaseUrl;


    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto requestDto, User user) {

        Orders order = ordersRepository.findByOrderId(requestDto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("오더가 없습니다"));

        // 결제 생성은 결제 의사를 등록하는 단계일 뿐이다.
        // 여기서 완료로 바꾸면 결제하지 않고 이탈해도 주문이 완료로 남아
        // 미결제 주문 정리 대상에서 빠지고 재고가 영영 묶인다.

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


    /**
     * 결제 승인.
     * 토스 호출을 트랜잭션 밖에서 하고, 성공한 뒤에만 DB 에 반영한다.
     * 예전에는 호출 전에 이미 "결제 완료"로 기록했다.
     */
    @Override
    public void approvePayment(String paymentKey, String orderId, Long amount) throws JsonProcessingException {
        paymentStateWriter.verifyApprovable(orderId, amount);

        PayloadRequestDto payload = new PayloadRequestDto(orderId, String.valueOf(amount));
        callToss("/" + paymentKey, payload);

        paymentStateWriter.applyApproval(orderId, paymentKey);
    }

    /**
     * 결제 취소.
     * 토스 호출을 트랜잭션 밖에서 하고, 성공한 뒤에만 주문 취소와 재고 복원을 반영한다.
     */
    @Override
    public void cancelPayment(String paymentKey, String cancelReason, User requester) throws JsonProcessingException {
        Long orderId = paymentStateWriter.verifyCancellable(paymentKey, requester);

        callToss("/" + paymentKey + "/cancel", new PayloadRequestDto(cancelReason));

        paymentStateWriter.applyCancellation(paymentKey, orderId, cancelReason);
    }

    /** 토스 API 호출. 실패하면 예외가 올라와 DB 반영이 일어나지 않는다. */
    private void callToss(String path, PayloadRequestDto payload) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes()));
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
        restTemplate.postForEntity(tossBaseUrl + "/payments" + path, request, JsonNode.class);
    }

    @Transactional
    public UserOrderDto findItem(Long userId, Long orderId, User requester) {
        // 경로의 userId 를 그대로 믿으면 남의 주문을 들여다볼 수 있었다
        if (!requester.getUserId().equals(userId)) {
            throw new NoAuthorizedException(ErrorCode.NO_AUTHORITY);
        }
        User user = userRepository.findByIdOrElseThrow(userId);
        Orders order = ordersRepository.findByIdOrElseThrow(orderId);
        return new UserOrderDto(user, order);
    }

    @Transactional
    @Override
    /**
     * 부분 취소 호출.
     * DB 를 건드리지 않는데 트랜잭션이 걸려 있어 결제사 응답을 기다리는 동안
     * 커넥션만 붙잡고 있었다. 조회해 온 결제 정보도 쓰이지 않았다.
     */
    public void partialCancelPayment(String paymentKey, BigDecimal cancelAmount, String cancelReason) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes()));
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = objectMapper.writeValueAsString(new PartialCancelRequest(cancelAmount, cancelReason));
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        restTemplate.postForEntity(tossBaseUrl + "/payments/" + paymentKey + "/cancel", request, JsonNode.class);
    }

    // 부분 취소 요청을 위한 내부 클래스
    private static class PartialCancelRequest {
        public final BigDecimal cancelAmount;
        public final String cancelReason;

        public PartialCancelRequest(BigDecimal cancelAmount, String cancelReason) {
            this.cancelAmount = cancelAmount;
            this.cancelReason = cancelReason;
        }
    }
}