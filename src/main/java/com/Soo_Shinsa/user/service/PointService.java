package com.Soo_Shinsa.user.service;

import com.Soo_Shinsa.exception.ErrorCode;
import com.Soo_Shinsa.exception.InvalidInputException;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.user.model.Grade;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.model.UserGrade;
import com.Soo_Shinsa.user.repository.GradeRepository;
import com.Soo_Shinsa.user.repository.UserGradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

/**
 * 포인트 적립/사용과 등급 승급.
 * - 사용: 주문 생성 때 결제 금액에서 차감. 잔액·주문 금액을 넘으면 거절.
 * - 적립: 결제 완료(PAYMENTCOMPLETED) 시 결제 금액 × 등급 적립률. 주문당 한 번만(pointSettled).
 * - 승급: 누적 구매액이 grade.requirement 를 넘으면 자동. 강등은 없음.
 * - 취소/반품: 사용 포인트 반환 + 적립 포인트 회수. 주문당 한 번만(pointRefunded).
 * 호출자는 영속 상태의 User/Orders 를 트랜잭션 안에서 넘겨야 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {
    private final GradeRepository gradeRepository;
    private final UserGradeRepository userGradeRepository;

    /** 주문 생성 시 포인트 사용. 0 이하면 무시. */
    public BigDecimal use(User user, BigDecimal amount, BigDecimal orderTotal) {
        if (amount == null || amount.signum() <= 0) return BigDecimal.ZERO;
        if (amount.compareTo(user.getPoint() == null ? BigDecimal.ZERO : user.getPoint()) > 0) throw new InvalidInputException(ErrorCode.NOT_ENOUGH_POINT);
        if (amount.compareTo(orderTotal) > 0) throw new InvalidInputException(ErrorCode.POINT_OVER_ORDER_TOTAL);
        user.subtractPoint(amount);
        return amount;
    }

    /** 결제 완료 시 적립 + 누적 구매액 + 승급. 이미 정산된 주문이면 무시. */
    public void settlePurchase(Orders order) {
        if (order.isPointSettled()) return;
        User user = order.getUser();
        BigDecimal paid = order.getTotalPrice();
        BigDecimal earned = paid.multiply(currentRate(user)).setScale(2, RoundingMode.DOWN);
        user.addPoint(earned);
        user.addPurchase(paid);
        order.recordEarnedPoint(earned);
        upgradeIfEligible(user);
    }

    /** 취소/반품 시 되돌리기. 결제 전 취소면 사용 포인트만 돌려준다. 이미 되돌린 주문이면 무시. */
    public void rollbackForCancel(Orders order) {
        if (order.isPointRefunded()) return;
        User user = order.getUser();
        if (order.getUsedPoint() != null && order.getUsedPoint().signum() > 0) user.addPoint(order.getUsedPoint());
        if (order.isPointSettled()) {
            user.subtractPoint(order.getEarnedPoint());
            user.subtractPurchase(order.getTotalPrice());
        }
        order.markPointRefunded();
    }

    /** 상태 전이에 따라 정산/되돌리기를 결정한다 (OrdersServiceImpl, Toss 승인/취소가 공통으로 호출). */
    public void onStatusChanged(Orders order, com.Soo_Shinsa.constant.OrdersStatus to) {
        switch (to) {
            case PAYMENTCOMPLETED -> settlePurchase(order);
            case CANCEL, RETURN, RETURNCOMPLETED -> rollbackForCancel(order);
            default -> { }
        }
    }

    private BigDecimal currentRate(User user) {
        if (user.getUserGrade() == null || user.getUserGrade().getGrade() == null) return BigDecimal.ZERO;
        return user.getUserGrade().getGrade().getPointRate();
    }

    private void upgradeIfEligible(User user) {
        Grade best = gradeRepository.findAll().stream()
                .filter(g -> g.getRequirement().compareTo(user.getTotalPurchase()) <= 0)
                .max(Comparator.comparing(Grade::getRequirement)).orElse(null);
        if (best == null) return;
        Grade current = user.getUserGrade() == null ? null : user.getUserGrade().getGrade();
        if (current != null && current.getRequirement().compareTo(best.getRequirement()) >= 0) return;
        UserGrade upgraded = userGradeRepository.save(new UserGrade(best));
        user.updateUserGrade(upgraded);
        log.info("등급 승급: userId={} -> {}", user.getUserId(), best.getName());
    }
}
