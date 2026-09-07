package com.Soo_Shinsa.global.config;

import com.Soo_Shinsa.support.IntegrationTestSupport;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 비즈니스 메트릭이 실제로 레지스트리에 등록되는지 확인한다.
 * 이름이 틀리면 대시보드에서 조용히 비어 보이기만 한다.
 */
@SpringBootTest
class BusinessMetricsTest extends IntegrationTestSupport {

    @Autowired private BusinessMetrics metrics;
    @Autowired private MeterRegistry registry;

    private double count(String name) {
        return registry.get(name).counter().count();
    }

    @Test
    void 모든_지표가_등록된다() {
        for (String name : List.of(
                "sooshinsa.coupon.issued",
                "sooshinsa.coupon.sold_out",
                "sooshinsa.coupon.prefilter_rejected",
                "sooshinsa.order.stock_shortage",
                "sooshinsa.order.expired")) {
            assertNotNull(registry.find(name).counter(), "등록되지 않은 지표: " + name);
        }
    }

    @Test
    void 증가시키면_값이_올라간다() {
        double before = count("sooshinsa.coupon.issued");
        metrics.couponIssued();
        assertEquals(before + 1, count("sooshinsa.coupon.issued"));
    }

    @Test
    void 만료_주문은_건수만큼_한번에_올린다() {
        double before = count("sooshinsa.order.expired");
        metrics.orderExpired(5);
        assertEquals(before + 5, count("sooshinsa.order.expired"));
    }
}
