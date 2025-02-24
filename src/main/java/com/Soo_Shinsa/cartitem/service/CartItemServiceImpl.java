package com.Soo_Shinsa.cartitem.service;

import com.Soo_Shinsa.cartitem.dto.*;
import com.Soo_Shinsa.cartitem.model.CartItem;
import com.Soo_Shinsa.cartitem.repository.CartItemRepository;
import com.Soo_Shinsa.constant.ProductStatus;
import com.Soo_Shinsa.coupon.calculate.DiscountCouponCalculator;
import com.Soo_Shinsa.coupon.calculate.PercentageDiscountCalculator;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponUser;
import com.Soo_Shinsa.coupon.repository.CouponBrandRelationRepository;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartItemServiceImpl implements CartItemService {

    private final CartItemRepository cartItemRepository;
    private final ProductOptionRepository productOptionRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CouponUserRepository couponUserRepository;
    private final CouponRepository couponRepository;
    private final CouponBrandRelationRepository couponBrandRelationRepository;

    @Transactional
    @Override
    public CartItemResponseDto create(User user, CartItemRequestDto requestDto) {

        Product product = productRepository.findByIdOrElseThrow(requestDto.getProductId());

        if (product.getProductStatus().equals(ProductStatus.SOLD_OUT) || product.getProductStatus().equals(ProductStatus.UNAVAILABLE)) {
            throw new InternalServerException(ErrorCode.CAN_NOT_USE_PRODUCT);
        }

        List<ProductOption> productOptions = productOptionRepository.findProductOptionByProductId(product.getId());

        CartItem cartItem = CartItem.builder()
                .quantity(requestDto.getQuantity())
                .user(user)
                .product(product)
                .productOption(productOptions.get(0))
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

        List<ProductOption> productOptions = productOptionRepository.findProductOptionByProductId(cartItem.getProduct().getId());

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
        List<ProductOption> productOptions = productOptionRepository.findProductOptionByProductId(cartItem.getProduct().getId());

        return CartItemResponseDto.toDto(cartItem, productOptions);
    }

    @Transactional
    public ApplyCouponCartResponseDto applyCoupon(Long cartId, ApplyCouponCartRequestDto requestDto, User user) {
        CartItem cartItem = cartItemRepository.findByIdOrElseThrow(cartId);

        Optional<CouponUser> existingCouponUser = couponUserRepository.findByCouponIdAndUserUserId(requestDto.getCouponId(), user.getUserId());
        if (existingCouponUser.isPresent() && existingCouponUser.get().isUsed()) {
            throw new InvalidInputException(ErrorCode.ALREADY_USED_COUPON);
        }

        // 사용되지 않은 쿠폰이 있는지 확인
        Coupon coupon;
        CouponUser couponUser;

        Optional<CouponUser> optionalCouponUser = couponUserRepository.findUnusedCouponByCouponId(requestDto.getCouponId());
        if (optionalCouponUser.isPresent()) {
            couponUser = optionalCouponUser.get();
            coupon = couponUser.getCoupon();
        } else {
            coupon = couponRepository.findById(requestDto.getCouponId())
                    .orElseThrow(() -> new InvalidInputException(ErrorCode.NOT_FOUND_COUPON));

            if (coupon.getMaxCount() <= 0) {
                throw new InvalidInputException(ErrorCode.COUPON_OUT_OF_STOCK);
            }

            couponUser = CouponUser.builder()
                    .coupon(coupon)
                    .user(user)
                    .isUsed(false)
                    .usedAt(null)
                    .build();

            couponUserRepository.saveAndFlush(couponUser);
        }

        if (coupon.isExpired()) {
            throw new InvalidInputException(ErrorCode.EXPIRED_COUPON);
        }

        Long brandId = cartItem.getProduct().getBrand().getId();
        boolean isApplicable = couponBrandRelationRepository.existsByCouponAndBrand(coupon, cartItem.getProduct().getBrand());

        if (!isApplicable) {
            log.error("❌ 쿠폰 적용 실패: 브랜드가 일치하지 않습니다. 브랜드 ID: {}, 쿠폰 ID: {}", brandId, coupon.getId());
            throw new InvalidInputException(ErrorCode.NOT_APPLICABLE_COUPON);
        }

        DiscountCouponCalculator discountCouponCalculator = new PercentageDiscountCalculator();
        BigDecimal discountPrice = discountCouponCalculator.calculateDiscountedPrice(cartItem.getProduct().getPrice(), coupon.getDiscountRate());

        if (discountPrice == null || discountPrice.compareTo(BigDecimal.ZERO) <= 0) {
            discountPrice = cartItem.getProduct().getPrice();
        }

        cartItem.applyCoupon(coupon, discountPrice);
        cartItemRepository.saveAndFlush(cartItem);

        List<ProductOption> productOptions = productOptionRepository.findProductOptionByProductId(cartItem.getProduct().getId());
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