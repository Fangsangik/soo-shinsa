package com.Soo_Shinsa.coupon.service;

import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.constant.CouponType;
import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.coupon.dto.CouponResponseDto;
import com.Soo_Shinsa.coupon.dto.MyCouponResponseDto;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.repository.CouponRepository;
import com.Soo_Shinsa.coupon.repository.CouponUserRepository;
import com.Soo_Shinsa.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static com.Soo_Shinsa.support.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplUnitTest {
    @Mock CouponRepository couponRepository;
    @Mock BrandRepository brandRepository;
    @Mock CouponUserRepository couponUserRepository;
    @InjectMocks CouponServiceImpl service;

    @Test
    void 사용_가능_목록은_내가_이미_쓴_쿠폰을_제외한다() {
        User me = user(1L, Role.CUSTOMER);
        Coupon a = coupon(1L, 10, CouponType.UNIVERSAL, 5), b = coupon(2L, 20, CouponType.UNIVERSAL, 5), c = coupon(3L, 30, CouponType.UNIVERSAL, 5);
        when(couponRepository.findAllByExpirationDateGreaterThanEqualAndMaxCountGreaterThan(any(LocalDate.class), eq(0))).thenReturn(List.of(a, b, c));
        when(couponUserRepository.findUsedCouponIdsByUserId(1L)).thenReturn(List.of(2L));

        List<CouponResponseDto> result = service.findAvailable(me);

        assertThat(result).extracting(CouponResponseDto::getId).containsExactly(1L, 3L);
        assertThat(result.get(0).getDiscountRate()).isEqualByComparingTo("10");
    }

    @Test
    void 내_쿠폰은_사용_여부와_함께_내려간다() {
        User me = user(1L, Role.CUSTOMER);
        Coupon a = coupon(1L, 10, CouponType.UNIVERSAL, 5);
        when(couponUserRepository.findAllByUserId(1L)).thenReturn(List.of(couponUser(1L, a, me, true), couponUser(2L, a, me, false)));

        List<MyCouponResponseDto> mine = service.findMine(me);

        assertThat(mine).extracting(MyCouponResponseDto::isUsed).containsExactly(true, false);
        assertThat(mine.get(0).getCouponName()).isEqualTo("c1");
    }
}
