package com.Soo_Shinsa.user.service;

import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.user.model.Grade;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.model.UserGrade;
import com.Soo_Shinsa.user.repository.GradeRepository;
import com.Soo_Shinsa.user.repository.UserGradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

/**
 * 포인트 적립/사용과 등급 승급.
 *
 * 정책:
 * - 사용: 주문 생성 때 결제 금액에서 차감한다. 잔액을 넘거나 주문 금액을 넘으면 거절.
 * - 적립: 결제 승인 때 (결제 금액 × 등급 적립률). 사용/적립액은 주문에 기록해 둔다.
 * - 승급: 누적 구매액이 grade.requirement 를 넘으면 자동. 취소로 내려가도 강등은 하지 않는다.
 * - 취소: 주문에 기록된 사용 포인트는 돌려주고 적립 포인트는 회수한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointService {

    private final GradeRepository gradeRepository;
    private final UserGradeRepository userGradeRepository;

    /** 주문 생성 시 포인트 사용. 사용액만큼 결제 금액이 줄어든다. */
    public void use(User user, BigDecimal amount, BigDecimal orderTotal) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        if (amount.compareTo(user.getPoint()) > 0) {
            throw new InvalidInputException(ErrorCode.NOT_ENOUGH_POINT);
        }
        if (amount.compareTo(orderTotal) > 0) {
            throw new InvalidInputException(ErrorCode.POINT_OVER_ORDER_TOTAL);
        }
        user.subtractPoint(amount);
    }

    /** 결제 승인 시: 적립 + 누적 구매액 반영 + 승급 판정. */
    @Transactional
    public void settlePurchase(Orders order) {
        User user = order.getUser();
        BigDecimal paid = order.getTotalPrice();

        BigDecimal rate = currentRate(user);
        BigDecimal earned = paid.multiply(rate).setScale(2, RoundingMode.DOWN);

        user.addPoint(earned);
        user.addPurchase(paid);
        order.recordEarnedPoint(earned);

        upgradeIfEligible(user);
    }

    /** 주문 취소 시: 사용 포인트 반환, 적립 포인트 회수. 강등은 하지 않는다. */
    @Transactional
    public void rollbackForCancel(Orders order) {
        User user = order.getUser();
        if (order.getUsedPoint() != null && order.getUsedPoint().signum() > 0) {
            user.addPoint(order.getUsedPoint());
        }
        if (order.getEarnedPoint() != null && order.getEarnedPoint().signum() > 0) {
            user.subtractPoint(order.getEarnedPoint());
            user.subtractPurchase(order.getTotalPrice());
        }
    }

    private BigDecimal currentRate(User user) {
        if (user.getUserGrade() == null || user.getUserGrade().getGrade() == null) {
            return BigDecimal.ZERO;
        }
        return user.getUserGrade().getGrade().getPointRate();
    }

    private void upgradeIfEligible(User user) {
        Grade best = gradeRepository.findAll().stream()
                .filter(g -> g.getRequirement().compareTo(user.getTotalPurchase()) <= 0)
                .max(Comparator.comparing(Grade::getRequirement))
                .orElse(null);
        if (best == null) {
            return;
        }
        Grade current = user.getUserGrade() == null ? null : user.getUserGrade().getGrade();
        if (current != null && current.getRequirement().compareTo(best.getRequirement()) >= 0) {
            return;
        }
        UserGrade upgraded = new UserGrade(best);
        userGradeRepository.save(upgraded);
        user.updateUserGrade(upgraded);
        log.info("등급 승급: userId={} -> {}", user.getUserId(), best.getName());
    }
}
