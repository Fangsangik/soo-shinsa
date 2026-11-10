package com.Soo_Shinsa.cartitem.controller;


import com.Soo_Shinsa.cartitem.dto.*;
import com.Soo_Shinsa.cartitem.service.CartItemService;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.utils.CommonResponse;
import com.Soo_Shinsa.utils.ResponseMessage;
import com.Soo_Shinsa.utils.UserUtils;
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
public class CartItemController {
    private final CartItemService cartItemService;

    @PostMapping
    public ResponseEntity<CommonResponse<CartItemResponseDto>> createCart(@AuthenticationPrincipal UserDetails userDetails,
                                                                          @Valid @RequestBody CartItemRequestDto dto) {
        User user = UserUtils.getUser(userDetails);
        CartItemResponseDto saved = cartItemService.create(user, dto);
        CommonResponse<CartItemResponseDto> response = new CommonResponse<>(ResponseMessage.CART_CREATE_SUCCESS, saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/{cartId}")
    public ResponseEntity<CommonResponse<CartItemResponseDto>> findById(@AuthenticationPrincipal UserDetails userDetails,
                                                                        @PathVariable Long cartId) {
        User user = UserUtils.getUser(userDetails);
        CartItemResponseDto findCart = cartItemService.findById(cartId, user);
        CommonResponse<CartItemResponseDto> response = new CommonResponse<>(ResponseMessage.CART_SELECT_SUCCESS, findCart);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    //유저의 카트들을 모두 검색
    @GetMapping("/users")
    public ResponseEntity<CommonResponse<Page<CartItemResponseDto>>> findByIdAll(@AuthenticationPrincipal UserDetails userDetails,
                                                                                 @ModelAttribute CartItemDateRequestDto requestDto,
                                                                                 @RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "10") int size) {

        User user = UserUtils.getUser(userDetails);
        Page<CartItemResponseDto> cartItems = cartItemService.findByAll(user, requestDto, page, size);
        CommonResponse<Page<CartItemResponseDto>> response = new CommonResponse<>(ResponseMessage.CART_SELECT_SUCCESS, cartItems);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @PatchMapping("/{cartId}")
    public ResponseEntity<CommonResponse<CartItemResponseDto>> update(@AuthenticationPrincipal UserDetails userDetails,
                                                                      @PathVariable Long cartId,
                                                                      @Valid @RequestBody CartItemRequestDto dto) {
        User user = UserUtils.getUser(userDetails);
        CartItemResponseDto saved = cartItemService.update(cartId, user, dto.getQuantity());
        CommonResponse<CartItemResponseDto> response = new CommonResponse<>(ResponseMessage.CART_UPDATE_SUCCESS, saved);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long cartId) {
        User user = UserUtils.getUser(userDetails);
        cartItemService.delete(cartId, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("{cartId}/apply-coupon")
    public ResponseEntity<CommonResponse<ApplyCouponCartResponseDto>> applyCoupon(@AuthenticationPrincipal UserDetails userDetails,
                                                                                  @PathVariable Long cartId,
                                                                                  @RequestBody ApplyCouponCartRequestDto requestDto) {
        User user = UserUtils.getUser(userDetails);
        ApplyCouponCartResponseDto saved = cartItemService.applyCoupon(cartId, requestDto, user);
        CommonResponse<ApplyCouponCartResponseDto> response = new CommonResponse<>(ResponseMessage.COUPON_APPLIED, saved);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}