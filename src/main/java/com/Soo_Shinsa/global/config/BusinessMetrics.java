package com.Soo_Shinsa.global.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 비즈니스 메트릭.
 *
 * Prometheus/Grafana 는 붙어 있었지만 HTTP 지표만 수집하고 있어서
 * "선착순이 언제 마감됐는지", "재고 부족으로 몇 건이 떨어졌는지" 를 볼 수 없었다.
 */
@Component
public class BusinessMetrics {

    private final Counter couponIssued;
    private final Counter couponSoldOut;
    private final Counter couponPrefilterRejected;
    private final Counter orderStockShortage;
    private final Counter orderExpired;

    public BusinessMetrics(MeterRegistry registry) {
        this.couponIssued = Counter.builder("sooshinsa.coupon.issued")
                .description("선착순 쿠폰 발급 성공 건수").register(registry);
        this.couponSoldOut = Counter.builder("sooshinsa.coupon.sold_out")
                .description("정원 소진으로 발급 거절된 건수").register(registry);
        this.couponPrefilterRejected = Counter.builder("sooshinsa.coupon.prefilter_rejected")
                .description("Redis 선차단으로 DB 까지 가지 않은 건수").register(registry);
        this.orderStockShortage = Counter.builder("sooshinsa.order.stock_shortage")
                .description("재고 부족으로 실패한 주문 건수").register(registry);
        this.orderExpired = Counter.builder("sooshinsa.order.expired")
                .description("미결제로 취소되고 재고가 반환된 주문 건수").register(registry);
    }

    public void couponIssued() {
        couponIssued.increment();
    }

    public void couponSoldOut() {
        couponSoldOut.increment();
    }

    public void couponPrefilterRejected() {
        couponPrefilterRejected.increment();
    }

    public void orderStockShortage() {
        orderStockShortage.increment();
    }

    public void orderExpired(int count) {
        orderExpired.increment(count);
    }
}
