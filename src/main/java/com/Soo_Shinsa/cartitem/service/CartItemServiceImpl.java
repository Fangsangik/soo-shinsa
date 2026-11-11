package com.Soo_Shinsa.cartitem.service;

import com.Soo_Shinsa.cartitem.dto.*;
import com.Soo_Shinsa.cartitem.model.CartItem;
import com.Soo_Shinsa.cartitem.repository.CartItemRepository;
import com.Soo_Shinsa.constant.ProductStatus;
import com.Soo_Shinsa.coupon.aop.CouponLock;
import com.Soo_Shinsa.coupon.calculate.DiscountCouponCalculator;
import com.Soo_Shinsa.coupon.calculate.PercentageDiscountCalculator;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponUser;
import com.Soo_Shinsa.coupon.repository.CouponRepository;
import com.Soo_Shinsa.coupon.repository.CouponUserRepository;
import com.Soo_Shinsa.exception.ErrorCode;
import com.Soo_Shinsa.exception.InternalServerException;
import com.Soo_Shinsa.exception.InvalidInputException;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import com.Soo_Shinsa.product.repository.ProductOptionRepository;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import com.Soo_Shinsa.utils.EntityValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartItemServiceImpl implements CartItemService {

    private final CartItemRepository cartItemRepository;
    private final ProductOptionRepository productOptionRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CouponUserRepository couponUserRepository;
    private final CouponRepository couponRepository;

    @Transactional
    @Override
    public CartItemResponseDto create(User user, CartItemRequestDto requestDto) {

        Product product = productRepository.findByIdOrElseThrow(requestDto.getProductId());

        if (product.getProductStatus().equals(ProductStatus.SOLD_OUT) || product.getProductStatus().equals(ProductStatus.UNAVAILABLE)) {
            throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        ProductOption selected = productOptionRepository.findByIdOrElseThrow(requestDto.getProductOptionId());
        if (!selected.getProduct().getId().equals(product.getId())) {
            throw new InternalServerException(ErrorCode.NOT_FOUND_PRODUCT_OPTION);
        }
        List<ProductOption> productOptions = List.of(selected);

        CartItem cartItem = CartItem.builder()
                .quantity(requestDto.getQuantity())
                .user(user)
                .product(product)
                .productOption(selected)
                .build();

        cartItemRepository.save(cartItem);

        return CartItemResponseDto.toDto(cartItem, productOptions);
    }

    @Override
    public CartItemResponseDto findById(Long cartId, User user) {
        // 사용자 정보 가져오기
        User userId = userRepository.findByIdOrElseThrow(user.getUserId());

        CartItem cartItem = cartItemRepository.findByIdOrElseThrow(cartId);

        //사용자의 카트인지 확인
        EntityValidator.validateUserOwnership(userId.getUserId(), cartItem.getUser().getUserId());

        List<ProductOption> productOptions = List.of(cartItem.getProductOption());

        return CartItemResponseDto.toDto(cartItem, productOptions);
    }

    @Override
    public Page<CartItemResponseDto> findByAll(User user, CartItemDateRequestDto requestDto, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return cartItemRepository.findByAllCartItem(user, requestDto, pageable);
    }


    @Transactional
    @Override
    public CartItemResponseDto update(Long cartId, User user, Integer quantity) {
        User userId = userRepository.findByIdOrElseThrow(user.getUserId());

        CartItem cartItem = cartItemRepository.findByIdOrElseThrow(cartId);

        EntityValidator.validateUserOwnership(userId.getUserId(), cartItem.getUser().getUserId());
        cartItem.updateCartItem(quantity);


        // 상품 옵션 조회
        List<ProductOption> productOptions = List.of(cartItem.getProductOption());

        return CartItemResponseDto.toDto(cartItem, productOptions);
    }

    @CouponLock(key = "'lock:coupon:' + #couponRequestDto.couponId")
    @Transactional
    public ApplyCouponCartResponseDto applyCoupon(Long cartId, ApplyCouponCartRequestDto requestDto, User user) {
        CartItem cartItem = cartItemRepository.findByIdOrElseThrow(cartId);
        EntityValidator.validateUserOwnership(user.getUserId(), cartItem.getUser().getUserId());

        // 이 사용자의 발급 내역만 본다 (다른 사용자의 미사용 쿠폰을 가져다 쓰던 버그 수정)
        Optional<CouponUser> mine = couponUserRepository.findByCouponIdAndUserUserId(requestDto.getCouponId(), user.getUserId());
        if (mine.isPresent() && mine.get().isUsed()) {
            throw new InvalidInputException(ErrorCode.ALREADY_USED_COUPON);
        }

        Coupon coupon = couponRepository.findById(requestDto.getCouponId())
                .orElseThrow(() -> new InvalidInputException(ErrorCode.NOT_FOUND_COUPON));
        if (coupon.isExpired()) {
            throw new InvalidInputException(ErrorCode.EXPIRED_COUPON);
        }

        // 쿠폰 종류(전체/특정 브랜드/특정 상품) 검증 + 할인가 계산
        DiscountCouponCalculator discountCouponCalculator = new PercentageDiscountCalculator();
        BigDecimal discountPrice = discountCouponCalculator.applyCoupon(coupon, cartItem.getProduct(), cartItem.getProduct().getPrice());

        CouponUser couponUser = mine.orElseGet(() -> {
            if (coupon.getMaxCount() <= 0) {
                throw new InvalidInputException(ErrorCode.COUPON_OUT_OF_STOCK);
            }
            coupon.decreaseMaxCount(1);  // 새로 발급되는 경우에만 재고 감소
            return couponUserRepository.save(CouponUser.builder().coupon(coupon).user(user).isUsed(false).usedAt(null).build());
        });

        cartItem.applyCoupon(coupon, discountPrice);
        cartItemRepository.save(cartItem);

        couponUser.markAsUsed();
        couponUserRepository.save(couponUser);

        // 응답 DTO 반환
        List<ProductOption> productOptions = List.of(cartItem.getProductOption());
        return ApplyCouponCartResponseDto.toDto(cartItem, productOptions);
    }


    @Transactional
    @Override
    public void delete(Long cartId, User user) {
        User userId = userRepository.findByIdOrElseThrow(user.getUserId());

        CartItem cartItem = cartItemRepository.findByIdOrElseThrow(cartId);

        EntityValidator.validateUserOwnership(userId.getUserId(), cartItem.getUser().getUserId());

        cartItemRepository.delete(cartItem);
    }
}