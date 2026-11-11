package com.Soo_Shinsa.cartitem.service;

import com.Soo_Shinsa.cartitem.dto.ApplyCouponCartRequestDto;
import com.Soo_Shinsa.cartitem.dto.ApplyCouponCartResponseDto;
import com.Soo_Shinsa.cartitem.dto.CartItemRequestDto;
import com.Soo_Shinsa.cartitem.dto.CartItemResponseDto;
import com.Soo_Shinsa.cartitem.model.CartItem;
import com.Soo_Shinsa.cartitem.repository.CartItemRepository;
import com.Soo_Shinsa.constant.CouponType;
import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponBrandRelation;
import com.Soo_Shinsa.coupon.model.CouponUser;
import com.Soo_Shinsa.coupon.repository.CouponRepository;
import com.Soo_Shinsa.coupon.repository.CouponUserRepository;
import com.Soo_Shinsa.exception.*;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import com.Soo_Shinsa.product.repository.ProductOptionRepository;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static com.Soo_Shinsa.support.Fixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartItemServiceImplTest {

    @Mock CartItemRepository cartItemRepository;
    @Mock ProductOptionRepository productOptionRepository;
    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;
    @Mock CouponUserRepository couponUserRepository;
    @Mock CouponRepository couponRepository;
    @InjectMocks CartItemServiceImpl service;

    User me, other, vendor;
    Product product, otherProduct;
    ProductOption option;

    @BeforeEach
    void setUp() {
        me = user(1L, Role.CUSTOMER);
        other = user(2L, Role.CUSTOMER);
        vendor = user(3L, Role.VENDOR);
        product = product(10L, brand(100L, vendor), 10_000);
        otherProduct = product(11L, brand(101L, vendor), 5_000);
        option = option(1L, product);
    }

    private CartItemRequestDto req(Long productId, Long optionId, int qty) {
        CartItemRequestDto d = new CartItemRequestDto();
        ReflectionTestUtils.setField(d, "productId", productId);
        ReflectionTestUtils.setField(d, "productOptionId", optionId);
        ReflectionTestUtils.setField(d, "quantity", qty);
        return d;
    }

    @Nested
    class 담기 {
        @Test
        void 선택한_옵션이_그_상품의_옵션이_아니면_거부() {
            when(productRepository.findByIdOrElseThrow(10L)).thenReturn(product);
            when(productOptionRepository.findByIdOrElseThrow(99L)).thenReturn(option(99L, otherProduct));
            assertThatThrownBy(() -> service.create(me, req(10L, 99L, 1))).isInstanceOf(InternalServerException.class);
            verify(cartItemRepository, never()).save(any());
        }

        @Test
        void 선택한_옵션_하나만_저장되고_응답에도_그_옵션만_온다() {
            when(productRepository.findByIdOrElseThrow(10L)).thenReturn(product);
            when(productOptionRepository.findByIdOrElseThrow(1L)).thenReturn(option);

            CartItemResponseDto dto = service.create(me, req(10L, 1L, 3));

            ArgumentCaptor<CartItem> saved = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository).save(saved.capture());
            assertThat(saved.getValue().getProductOption()).isSameAs(option);
            assertThat(saved.getValue().getQuantity()).isEqualTo(3);
            assertThat(dto.getProductOptions()).extracting("id").containsExactly(1L);
            assertThat(dto.getProductPrice()).isEqualByComparingTo("10000");
        }
    }

    @Nested
    class 쿠폰_적용 {
        CartItem cart;
        Coupon coupon;

        @BeforeEach
        void setUp() {
            cart = cartItem(5L, me, product, option, 1);
            coupon = coupon(1L, 10, CouponType.UNIVERSAL, 100);
            lenient().when(cartItemRepository.findByIdOrElseThrow(5L)).thenReturn(cart);
            lenient().when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
            lenient().when(couponUserRepository.save(any(CouponUser.class))).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        void 남의_장바구니에는_적용_불가() {
            assertThatThrownBy(() -> service.applyCoupon(5L, new ApplyCouponCartRequestDto(1L), other)).isInstanceOf(NoAuthorizedException.class);
            verifyNoInteractions(couponRepository);
        }

        @Test
        void 이미_쓴_쿠폰은_거부() {
            when(couponUserRepository.findByCouponIdAndUserUserId(1L, me.getUserId())).thenReturn(Optional.of(couponUser(1L, coupon, me, true)));
            assertThatThrownBy(() -> service.applyCoupon(5L, new ApplyCouponCartRequestDto(1L), me))
                    .isInstanceOf(InvalidInputException.class).extracting("errorCode").isEqualTo(ErrorCode.ALREADY_USED_COUPON);
        }

        @Test
        void 만료된_쿠폰은_거부() {
            ReflectionTestUtils.setField(coupon, "expirationDate", LocalDate.now().minusDays(1));
            when(couponUserRepository.findByCouponIdAndUserUserId(1L, me.getUserId())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.applyCoupon(5L, new ApplyCouponCartRequestDto(1L), me))
                    .isInstanceOf(InvalidInputException.class).extracting("errorCode").isEqualTo(ErrorCode.EXPIRED_COUPON);
        }

        @Test
        void 특정_브랜드_쿠폰은_다른_브랜드_상품에_거부() {
            Coupon brandCoupon = coupon(2L, 20, CouponType.SPECIFIC_BRAND, 100);
            brandCoupon.getCouponBrandRelations().add(CouponBrandRelation.builder().coupon(brandCoupon).brand(brand(999L, vendor)).build());
            when(couponRepository.findById(2L)).thenReturn(Optional.of(brandCoupon));
            when(couponUserRepository.findByCouponIdAndUserUserId(2L, me.getUserId())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.applyCoupon(5L, new ApplyCouponCartRequestDto(2L), me))
                    .isInstanceOf(InvalidInputException.class).extracting("errorCode").isEqualTo(ErrorCode.NOT_APPROPRIATE_COUPON);
            assertThat(cart.getDiscountedPrice()).isNull();
        }

        @Test
        void 처음_쓰는_쿠폰은_발급되고_10퍼센트_할인되며_사용_처리된다() {
            when(couponUserRepository.findByCouponIdAndUserUserId(1L, me.getUserId())).thenReturn(Optional.empty());

            ApplyCouponCartResponseDto dto = service.applyCoupon(5L, new ApplyCouponCartRequestDto(1L), me);

            assertThat(dto.getOriginalPrice()).isEqualByComparingTo("10000");
            assertThat(dto.getDiscountedPrice()).isEqualByComparingTo("9000");
            assertThat(coupon.getMaxCount()).isEqualTo(99);
            ArgumentCaptor<CouponUser> cu = ArgumentCaptor.forClass(CouponUser.class);
            verify(couponUserRepository, times(2)).save(cu.capture());
            assertThat(cu.getValue().isUsed()).isTrue();
            assertThat(cu.getValue().getUser()).isSameAs(me);
        }

        @Test
        void 이미_발급받은_미사용_쿠폰은_재발급_없이_사용_처리된다() {
            CouponUser mine = couponUser(7L, coupon, me, false);
            when(couponUserRepository.findByCouponIdAndUserUserId(1L, me.getUserId())).thenReturn(Optional.of(mine));

            service.applyCoupon(5L, new ApplyCouponCartRequestDto(1L), me);

            assertThat(mine.isUsed()).isTrue();
            assertThat(coupon.getMaxCount()).isEqualTo(100);
            verify(couponUserRepository, times(1)).save(mine);
            verify(couponUserRepository, never()).findUnusedCouponByCouponId(any()); // 다른 사용자의 쿠폰을 끌어오지 않는다
        }

        @Test
        void 수량이_0이면_거부() {
            when(couponUserRepository.findByCouponIdAndUserUserId(1L, me.getUserId())).thenReturn(Optional.empty());
            ReflectionTestUtils.setField(coupon, "maxCount", 0);
            assertThatThrownBy(() -> service.applyCoupon(5L, new ApplyCouponCartRequestDto(1L), me))
                    .isInstanceOf(InvalidInputException.class).extracting("errorCode").isEqualTo(ErrorCode.COUPON_OUT_OF_STOCK);
        }
    }
}
