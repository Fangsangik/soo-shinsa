package com.Soo_Shinsa.order.service;


import com.Soo_Shinsa.order.dto.PaymentRequestDto;
import com.Soo_Shinsa.order.dto.PaymentResponseDto;
import com.Soo_Shinsa.order.dto.UserOrderDto;
import com.Soo_Shinsa.user.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.ui.Model;

import java.math.BigDecimal;



public interface TossPaymentsService {
    PaymentResponseDto createPayment(PaymentRequestDto requestDto, User user);
    void approvePayment(String paymentKey, String orderId, Long amount, Model model) throws JsonProcessingException;
    UserOrderDto findItem(Long userId, Long orderId);
    void cancelPayment(String paymentKey, String cancelReason) throws JsonProcessingException;
    void partialCancelPayment(String paymentKey, BigDecimal cancelAmount, String cancelReason) throws JsonProcessingException;
}
