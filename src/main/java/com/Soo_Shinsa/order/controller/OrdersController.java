package com.Soo_Shinsa.order.controller;


import com.Soo_Shinsa.global.utils.CommonResponse;
import com.Soo_Shinsa.global.utils.ResponseMessage;
import com.Soo_Shinsa.global.utils.UserUtils;
import com.Soo_Shinsa.order.dto.*;
import com.Soo_Shinsa.order.service.OrdersService;
import com.Soo_Shinsa.product.dto.SingleProductOrderRequestDto;
import com.Soo_Shinsa.user.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
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
@RequestMapping("/orders")
@Tag(name = "Orders API", description = "주문 관련 API")
public class OrdersController {
    private final OrdersService ordersService;

    //특정유저의 특정 오더 읽기
    @GetMapping("/{orderId}")
    @Operation(summary = "특정 주문 조회", description = "특정 사용자의 주문 정보를 조회합니다.")
    public ResponseEntity<CommonResponse<OrdersResponseDto>> getOrderById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        User user = UserUtils.getUser(userDetails);
        OrdersResponseDto responseDto = ordersService.getOrderById(orderId, user);
        CommonResponse<OrdersResponseDto> response = new CommonResponse<>(ResponseMessage.ORDER_SELECT_SUCCESS, responseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    //특정유저의 모든 오더 읽기
    @GetMapping("/users")
    @Operation(summary = "사용자 전체 주문 조회", description = "특정 사용자의 모든 주문을 조회합니다.")
    public ResponseEntity<CommonResponse<Page<OrdersResponseDto>>> getOrderByAll(@AuthenticationPrincipal UserDetails userDetails,
                                                                 @RequestBody OrderDateRequestDto dateRequestDto,
                                                                 @RequestParam (defaultValue = "0") int page,
                                                                 @RequestParam (defaultValue = "10") int size) {
        User user = UserUtils.getUser(userDetails);
        Page<OrdersResponseDto> allByUserId = ordersService.getAllByUserId(user, dateRequestDto, page, size);
        CommonResponse<Page<OrdersResponseDto>> response = new CommonResponse<>(ResponseMessage.ORDER_SELECT_SUCCESS, allByUserId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    //    단품 구매 생성
    @PostMapping("/single")
    @Operation(summary = "단품 구매 생성", description = "단일 상품을 구매하는 주문을 생성합니다.")
    public ResponseEntity<CommonResponse<OrdersResponseDto>> createSingleProductOrder(@AuthenticationPrincipal UserDetails userDetails,
                                                                      @Valid @RequestBody SingleProductOrderRequestDto requestDto) {
        User user = UserUtils.getUser(userDetails);
        OrdersResponseDto response = ordersService.createSingleProductOrder(user, requestDto.getProductId(), requestDto.getQuantity());
        CommonResponse<OrdersResponseDto> commonResponse = new CommonResponse<>(ResponseMessage.ORDER_CREATE_SUCCESS, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(commonResponse);
    }

    //카트에 담음 물건을 구매 생성
    @PostMapping("/carts/all")
    @Operation(summary = "장바구니 전체 주문 생성", description = "장바구니에 담긴 모든 상품을 주문합니다.")
    public ResponseEntity<CommonResponse<OrdersResponseDto>> createAllOrderFromCart(@AuthenticationPrincipal UserDetails userDetails) {
        User user = UserUtils.getUser(userDetails);
        OrdersResponseDto responseDto = ordersService.createAllOrderFromCart(user);
        CommonResponse<OrdersResponseDto> response = new CommonResponse<>(ResponseMessage.ORDER_CREATE_SUCCESS, responseDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //오더 수정
    @PatchMapping
    @Operation(summary = "주문 수정", description = "주문의 상태를 수정합니다.")
    public ResponseEntity<CommonResponse<OrdersResponseDto>> updateOrder(@AuthenticationPrincipal UserDetails userDetails,
                                                         @Valid @RequestBody OrdersUpdateRequestDto requestDto) {
        User user = UserUtils.getUser(userDetails);
        OrdersResponseDto responseDto = ordersService.updateOrder(user, requestDto.getOrderId(), requestDto.getStatus());
        CommonResponse<OrdersResponseDto> response = new CommonResponse<>(ResponseMessage.ORDER_UPDATE_SUCCESS, responseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/carts")
    @Operation(summary = "장바구니 개별 주문 생성", description = "장바구니에 담긴 특정 상품을 주문합니다.")
    public ResponseEntity<CommonResponse<OrdersResponseDto>> createOrderFromCart(@AuthenticationPrincipal UserDetails userDetails,
                                                                                 @RequestBody OrderCreateRequestDto requestDto) {
        User user = UserUtils.getUser(userDetails);
        OrdersResponseDto responseDto = ordersService.createSingleOrderCartItem(user, requestDto);
        CommonResponse<OrdersResponseDto> response = new CommonResponse<>(ResponseMessage.ORDER_CREATE_SUCCESS, responseDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("{orderId}/cancel")
    @Operation(summary = "주문 취소", description = "주문을 취소합니다.")
    public ResponseEntity<CommonResponse<String>> cancelOrder(@AuthenticationPrincipal UserDetails userDetails,
                                                              @PathVariable Long orderId) throws JsonProcessingException {
        User user = UserUtils.getUser(userDetails);
        ordersService.cancelOrder(user, orderId);
        CommonResponse<String> response = new CommonResponse<>(ResponseMessage.ORDER_CANCEL_SUCCESS, "주문이 취소되었습니다.");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("{orderId}/partial-cancel")
    @Operation(summary = "주문 부분 취소", description = "주문의 특정 상품을 부분 취소합니다.")
    public ResponseEntity<CommonResponse<PartialCancelResponseDto>> partialCancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId,
            @Valid @RequestBody PartialCancelRequestDto requestDto) throws JsonProcessingException {
        
        User user = UserUtils.getUser(userDetails);
        PartialCancelResponseDto responseDto = ordersService.partialCancelOrder(user, orderId, requestDto);
        CommonResponse<PartialCancelResponseDto> response = new CommonResponse<>(
            ResponseMessage.ORDER_CANCEL_SUCCESS, 
            responseDto
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 🚀 성능 최적화된 엔드포인트들
    @GetMapping("/{orderId}/optimized")
    @Operation(summary = "최적화된 주문 상세 조회", description = "N+1 문제를 해결한 빠른 주문 상세 조회입니다.")
    public ResponseEntity<CommonResponse<OrdersResponseDto>> getOrderByIdOptimized(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        User user = UserUtils.getUser(userDetails);
        OrdersResponseDto responseDto = ordersService.getOrderByIdOptimized(orderId, user);
        CommonResponse<OrdersResponseDto> response = new CommonResponse<>(ResponseMessage.ORDER_SELECT_SUCCESS, responseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/users/summary")
    @Operation(summary = "최적화된 주문 요약 목록", description = "경량 DTO를 사용한 빠른 주문 목록 조회입니다.")
    public ResponseEntity<CommonResponse<Page<OrderSummaryDto>>> getOrderSummariesOptimized(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody OrderDateRequestDto dateRequestDto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = UserUtils.getUser(userDetails);
        Page<OrderSummaryDto> orderSummaries = ordersService.getOrderSummariesByUserId(user, dateRequestDto, page, size);
        CommonResponse<Page<OrderSummaryDto>> response = new CommonResponse<>(ResponseMessage.ORDER_SELECT_SUCCESS, orderSummaries);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/users/optimized")
    @Operation(summary = "최적화된 주문 전체 목록", description = "2단계 조회를 사용한 빠른 주문 목록 조회입니다.")
    public ResponseEntity<CommonResponse<Page<OrdersResponseDto>>> getOrdersByUserOptimized(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody OrderDateRequestDto dateRequestDto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = UserUtils.getUser(userDetails);
        Page<OrdersResponseDto> allByUserId = ordersService.getAllByUserIdOptimized(user, dateRequestDto, page, size);
        CommonResponse<Page<OrdersResponseDto>> response = new CommonResponse<>(ResponseMessage.ORDER_SELECT_SUCCESS, allByUserId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
