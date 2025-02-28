package com.Soo_Shinsa.cartitem.controller;


import com.Soo_Shinsa.cartitem.dto.*;
import com.Soo_Shinsa.cartitem.service.CartItemService;
import com.Soo_Shinsa.global.utils.CommonResponse;
import com.Soo_Shinsa.global.utils.ResponseMessage;
import com.Soo_Shinsa.global.utils.UserUtils;
import com.Soo_Shinsa.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/carts")
@Tag(name = "Cart API", description = "장바구니 관련 API")
public class CartItemController {
    private final CartItemService cartItemService;

    @PostMapping
    @Operation(summary = "장바구니 추가", description = "사용자의 장바구니에 상품을 추가합니다.")
    public ResponseEntity<CommonResponse<CartItemResponseDto>> createCart(@AuthenticationPrincipal UserDetails userDetails,
                                                                          @Valid @RequestBody CartItemRequestDto dto) {
        User user = UserUtils.getUser(userDetails);
        CartItemResponseDto saved = cartItemService.create(user, dto);
        CommonResponse<CartItemResponseDto> response = new CommonResponse<>(ResponseMessage.CART_CREATE_SUCCESS, saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/{cartId}")
    @Operation(summary = "장바구니 조회", description = "특정 장바구니 상품을 조회합니다.")
    public ResponseEntity<CommonResponse<CartItemResponseDto>> findById(@AuthenticationPrincipal UserDetails userDetails,
                                                                        @PathVariable Long cartId) {
        User user = UserUtils.getUser(userDetails);
        CartItemResponseDto findCart = cartItemService.findById(cartId, user);
        CommonResponse<CartItemResponseDto> response = new CommonResponse<>(ResponseMessage.CART_SELECT_SUCCESS, findCart);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    //유저의 카트들을 모두 검색
    @GetMapping("/users")
    @Operation(summary = "사용자 장바구니 조회", description = "사용자의 모든 장바구니 상품을 조회합니다.")
    public ResponseEntity<CommonResponse<Page<CartItemResponseDto>>> findByIdAll(@AuthenticationPrincipal UserDetails userDetails,
                                                                                 @RequestBody CartItemDateRequestDto requestDto,
                                                                                 @RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "10") int size) {

        User user = UserUtils.getUser(userDetails);
        Page<CartItemResponseDto> cartItems = cartItemService.findByAll(user, requestDto, page, size);
        CommonResponse<Page<CartItemResponseDto>> response = new CommonResponse<>(ResponseMessage.CART_SELECT_SUCCESS, cartItems);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @PatchMapping
    @Operation(summary = "장바구니 수정", description = "장바구니 상품의 수량을 수정합니다.")
    public ResponseEntity<CommonResponse<CartItemResponseDto>> update(@AuthenticationPrincipal UserDetails userDetails,
                                                                      @Valid @RequestBody CartItemUpdateRequestDto dto) {
        User user = UserUtils.getUser(userDetails);
        CartItemResponseDto saved = cartItemService.update(user, dto);
        CommonResponse<CartItemResponseDto> response = new CommonResponse<>(ResponseMessage.CART_UPDATE_SUCCESS, saved);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{cartId}")
    @Operation(summary = "장바구니 삭제", description = "장바구니 상품을 삭제합니다.")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long cartId) {
        User user = UserUtils.getUser(userDetails);
        cartItemService.delete(cartId, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("{cartId}/apply-coupon")
    @Operation(summary = "쿠폰 적용", description = "장바구니에 쿠폰을 적용합니다.")
    public ResponseEntity<CommonResponse<ApplyCouponCartResponseDto>> applyCoupon(@AuthenticationPrincipal UserDetails userDetails,
                                                                                  @PathVariable Long cartId,
                                                                                  @RequestBody ApplyCouponCartRequestDto requestDto) {
        User user = UserUtils.getUser(userDetails);
        ApplyCouponCartResponseDto saved = cartItemService.applyCoupon(cartId, requestDto, user);
        CommonResponse<ApplyCouponCartResponseDto> response = new CommonResponse<>(ResponseMessage.COUPON_APPLIED, saved);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}