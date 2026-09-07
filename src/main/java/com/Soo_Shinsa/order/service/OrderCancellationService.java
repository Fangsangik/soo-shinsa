package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.global.constant.OrdersStatus;
import com.Soo_Shinsa.order.model.OrderItem;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import com.Soo_Shinsa.product.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 취소 시 상태 변경과 재고 복원.
 *
 * 취소 경로가 주문 취소 / 결제 취소 두 갈래인데 결제 취소 쪽은 재고를 되돌리지 않아
 * 그 경로로 취소하면 재고가 사라졌다. 한 곳으로 모아 양쪽이 같게 동작하도록 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancellationService {

    private final OrdersRepository ordersRepository;
    private final ProductOptionRepository productOptionRepository;
    private final OrderCacheService orderCacheService;

    /**
     * 아직 취소되지 않은 주문 아이템을 취소하고 재고를 되돌린다.
     * 이미 취소된 주문이면 아무것도 하지 않는다(중복 복원 방지).
     *
     * @return 실제로 취소 처리했으면 true
     */
    @Transactional
    public boolean cancel(Long orderId, String reason) {
        Orders order = ordersRepository.findByIdOrElseThrow(orderId);

        if (order.getStatus() == OrdersStatus.ORDERCANCEL) {
            return false;
        }

        // 상태 변경을 먼저, 재고 복원을 나중에 한다.
        // increaseStock 은 clearAutomatically=true 벌크 UPDATE 라 영속성 컨텍스트를 비운다.
        // 복원을 먼저 하면 뒤의 cancelOrderItem() 이 detach 된 엔티티에 쓰여 조용히 사라진다.
        // (flushAutomatically=true 이므로 벌크 UPDATE 직전에 상태 변경이 먼저 flush 된다)
        List<OrderItem> toCancel = order.getOrderItems().stream()
                .filter(orderItem -> !orderItem.isCancelled())
                .toList();
        toCancel.forEach(orderItem -> orderItem.cancelOrderItem(reason));
        order.updateStatus(OrdersStatus.ORDERCANCEL);
        ordersRepository.save(order);
        toCancel.forEach(this::restoreStock);
        orderCacheService.evictOrderCaches(order.getId(), order.getUser().getUserId());
        return true;
    }

    private void restoreStock(OrderItem orderItem) {
        int updated = productOptionRepository.increaseStock(
                orderItem.getProductOption().getId(), orderItem.getQuantity());

        if (updated == 0) {
            log.warn("재고 복원 실패 - 상품 옵션 ID: {}, 복원 수량: {}",
                    orderItem.getProductOption().getId(), orderItem.getQuantity());
        }
    }
}
