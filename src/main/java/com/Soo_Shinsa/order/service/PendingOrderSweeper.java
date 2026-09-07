package com.Soo_Shinsa.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 미결제 주문 정리.
 * 주문 시 재고를 즉시 차감하므로, 결제하지 않고 이탈한 주문의 재고를 주기적으로 되돌린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.order.pending-sweep-enabled", havingValue = "true", matchIfMissing = true)
public class PendingOrderSweeper {

    private final OrdersService ordersService;

    /** 한 번에 처리할 주문 수. 트랜잭션이 지나치게 길어지지 않게 끊는다. */
    @Value("${app.order.pending-sweep-batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.order.pending-sweep-interval:PT1M}")
    public void sweep() {
        try {
            ordersService.expirePendingOrders(batchSize);
        } catch (Exception e) {
            // 스케줄러가 예외로 죽으면 이후 실행이 멈춘다
            log.error("미결제 주문 정리 실패: {}", e.getMessage(), e);
        }
    }
}
