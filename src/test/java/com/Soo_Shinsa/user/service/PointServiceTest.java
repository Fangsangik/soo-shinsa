package com.Soo_Shinsa.user.service;

import com.Soo_Shinsa.constant.GradeType;
import com.Soo_Shinsa.constant.OrdersStatus;
import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.exception.ErrorCode;
import com.Soo_Shinsa.exception.InvalidInputException;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.user.model.Grade;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.model.UserGrade;
import com.Soo_Shinsa.user.repository.GradeRepository;
import com.Soo_Shinsa.user.repository.UserGradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static com.Soo_Shinsa.support.Fixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 포인트 사용/적립/되돌리기/승급 규칙. 금액은 BigDecimal 비교. */
@ExtendWith(MockitoExtension.class)
class PointServiceTest {
    @Mock GradeRepository gradeRepository;
    @Mock UserGradeRepository userGradeRepository;
    @InjectMocks PointService service;

    User me;
    Grade rookie = new Grade(GradeType.ROOKIE, new BigDecimal("0.01"), new BigDecimal("1000"));
    Grade bronze = new Grade(GradeType.BRONZE, new BigDecimal("0.02"), new BigDecimal("2000"));
    Grade gold = new Grade(GradeType.GOLD, new BigDecimal("0.05"), new BigDecimal("5000"));

    @BeforeEach
    void setUp() {
        me = user(1L, Role.CUSTOMER);
        me.updateUserGrade(new UserGrade(rookie));
        me.addPoint(new BigDecimal("5000"));
        lenient().when(gradeRepository.findAll()).thenReturn(List.of(rookie, bronze, gold));
        lenient().when(userGradeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Orders paidOrder(String total) {
        Orders o = order(1L, me, OrdersStatus.BEFOREPAYMENT);
        ReflectionTestUtils.setField(o, "totalPrice", new BigDecimal(total));
        return o;
    }

    @Test
    void 사용은_잔액과_주문금액을_넘을_수_없다() {
        assertThatThrownBy(() -> service.use(me, new BigDecimal("6000"), new BigDecimal("100000"))).isInstanceOf(InvalidInputException.class).extracting("errorCode").isEqualTo(ErrorCode.NOT_ENOUGH_POINT);
        assertThatThrownBy(() -> service.use(me, new BigDecimal("3000"), new BigDecimal("2000"))).isInstanceOf(InvalidInputException.class).extracting("errorCode").isEqualTo(ErrorCode.POINT_OVER_ORDER_TOTAL);
        assertThat(service.use(me, new BigDecimal("3000"), new BigDecimal("10000"))).isEqualByComparingTo("3000");
        assertThat(me.getPoint()).isEqualByComparingTo("2000");
        assertThat(service.use(me, null, BigDecimal.TEN)).isEqualByComparingTo("0");
    }

    @Test
    void 결제_완료_정산은_적립률대로_적립하고_한_번만_된다() {
        Orders o = paidOrder("10000");
        service.onStatusChanged(o, OrdersStatus.PAYMENTCOMPLETED);
        service.onStatusChanged(o, OrdersStatus.PAYMENTCOMPLETED); // 관리자가 같은 상태를 두 번 눌러도

        assertThat(o.getEarnedPoint()).isEqualByComparingTo("100");        // 10,000 × 1%
        assertThat(me.getPoint()).isEqualByComparingTo("5100");
        assertThat(me.getTotalPurchase()).isEqualByComparingTo("10000");
        assertThat(o.isPointSettled()).isTrue();
    }

    @Test
    void 누적_구매액이_기준을_넘으면_가장_높은_등급으로_승급하고_강등은_없다() {
        service.settlePurchase(paidOrder("4999"));
        assertThat(me.getUserGrade().getGrade().getName()).isEqualTo(GradeType.BRONZE);
        service.settlePurchase(paidOrder("1"));
        assertThat(me.getUserGrade().getGrade().getName()).isEqualTo(GradeType.GOLD);

        Orders big = paidOrder("5000"); service.settlePurchase(big);
        service.rollbackForCancel(big);
        assertThat(me.getUserGrade().getGrade().getName()).isEqualTo(GradeType.GOLD); // 취소로 내려가도 강등 없음
    }

    @Test
    void 취소는_사용_포인트를_돌려주고_적립을_회수하며_한_번만_된다() {
        Orders o = paidOrder("10000");
        o.applyPoint(service.use(me, new BigDecimal("1000"), new BigDecimal("10000"))); // 잔액 4000, 결제 9000
        service.settlePurchase(o);                                                     // +90 → 4090, 누적 9000

        service.onStatusChanged(o, OrdersStatus.CANCEL);
        service.onStatusChanged(o, OrdersStatus.RETURN); // 취소 완료 처리 때 또 호출돼도

        assertThat(me.getPoint()).isEqualByComparingTo("5000");
        assertThat(me.getTotalPurchase()).isEqualByComparingTo("0");
        assertThat(o.isPointRefunded()).isTrue();
    }

    @Test
    void 결제_전_취소는_사용_포인트만_돌려준다() {
        Orders o = paidOrder("10000");
        o.applyPoint(service.use(me, new BigDecimal("1000"), new BigDecimal("10000")));
        service.onStatusChanged(o, OrdersStatus.CANCEL);
        assertThat(me.getPoint()).isEqualByComparingTo("5000");
        assertThat(me.getTotalPurchase()).isEqualByComparingTo("0");
    }
}
