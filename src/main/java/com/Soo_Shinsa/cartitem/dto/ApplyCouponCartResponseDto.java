package com.Soo_Shinsa.cartitem.dto;

import com.Soo_Shinsa.cartitem.model.CartItem;
import com.Soo_Shinsa.product.dto.ProductOptionResponseDto;
import com.Soo_Shinsa.product.model.ProductOption;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class ApplyCouponCartResponseDto {

    private Long cartItemId;
    private Long productId;
    private BigDecimal originalTotalPrice;
    private BigDecimal discountedPrice; // 추가
    private List<ProductOptionResponseDto> productOptions;

    @Builder
    public ApplyCouponCartResponseDto(Long cartItemId, Long productId, BigDecimal originalTotalPrice,  BigDecimal discountedPrice, List<ProductOptionResponseDto> productOptions) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.originalTotalPrice = originalTotalPrice;
        this.discountedPrice = discountedPrice;
        this.productOptions = productOptions;
    }

    public static ApplyCouponCartResponseDto toDto(CartItem cartItem, List<ProductOption> productOptions) {
        // 1. 원래 총 가격 계산 (상품 가격 * 옵션 총 수량)
        int totalQuantity = productOptions.stream()
                .mapToInt(ProductOption::getQuantity) // 옵션별 수량 가져오기
                .sum(); // 모든 옵션의 수량 합산

        BigDecimal originalTotalPrice = cartItem.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(totalQuantity));

        return ApplyCouponCartResponseDto.builder()
                .cartItemId(cartItem.getId())
                .productId(cartItem.getProduct().getId())
                .originalTotalPrice(originalTotalPrice) // ✅ 원래 총 가격 설정 (상품 가격 * 총 옵션 수량)
                .discountedPrice(cartItem.getDiscountedPrice()) // 할인된 가격 설정
                .productOptions(productOptions.stream()
                        .map(ProductOptionResponseDto::toDto)
                        .collect(Collectors.toList()))
                .build();
    }

}
