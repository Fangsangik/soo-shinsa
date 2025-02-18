package com.Soo_Shinsa.order.controller;

import com.Soo_Shinsa.order.dto.OrderDateRequestDto;
import com.Soo_Shinsa.order.dto.OrderItemRequestDto;
import com.Soo_Shinsa.order.dto.OrderItemResponseDto;
import com.Soo_Shinsa.order.service.OrderItemService;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.utils.CommonResponse;
import com.Soo_Shinsa.utils.ResponseMessage;
import com.Soo_Shinsa.utils.UserUtils;
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
@RequestMapping("/orderitems")
@Tag(name = "OrderItem API", description = "주문 아이템 관련 API")
public class OrderItemController {
    private final OrderItemService orderItemService;

    //오더 아이템 생성
    @PostMapping
    @Operation(summary = "주문 아이템 생성", description = "새로운 주문 아이템을 생성합니다.")
    public ResponseEntity<CommonResponse<OrderItemResponseDto>> createOrderItem(@Valid @RequestBody OrderItemRequestDto requestDto,
                                                                               @AuthenticationPrincipal UserDetails userDetails) {
        User user = UserUtils.getUser(userDetails);
        OrderItemResponseDto orderItem = orderItemService.createOrderItem(requestDto, user);
        CommonResponse<OrderItemResponseDto> response = new CommonResponse<>(ResponseMessage.ORDER_ITEM_CREATE_SUCCESS, orderItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //특정유저의 특정 오더아이템 읽기
    @GetMapping("/{OrderItemsId}")
    @Operation(summary = "주문 아이템 조회", description = "특정 주문 아이템을 조회합니다.")
    public ResponseEntity<CommonResponse<OrderItemResponseDto>> findById(@PathVariable Long OrderItemsId,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = UserUtils.getUser(userDetails);
        OrderItemResponseDto findOrder = orderItemService.findById(OrderItemsId, user);
        CommonResponse<OrderItemResponseDto> response = new CommonResponse<>(ResponseMessage.ORDER_ITEM_SELECT_SUCCESS, findOrder);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    //특정 유저의 모든 오더아이템들을 읽기
    @GetMapping
    @Operation(summary = "모든 주문 아이템 조회", description = "특정 사용자의 모든 주문 아이템을 조회합니다.")
    public ResponseEntity<CommonResponse<Page<OrderItemResponseDto>>> readOrderItem(@AuthenticationPrincipal UserDetails userDetails,
                                                                    @RequestBody OrderDateRequestDto dateRequestDto,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        User user = UserUtils.getUser(userDetails);
        Page<OrderItemResponseDto> byAll = orderItemService.findByAll(user, dateRequestDto, page, size);
        CommonResponse<Page<OrderItemResponseDto>> response = new CommonResponse<>(ResponseMessage.ORDER_ITEM_SELECT_SUCCESS, byAll);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    //특정 유저의 특정 오더아이템 수정
    @PatchMapping("/{orderItemsId}")
    @Operation(summary = "주문 아이템 수정", description = "특정 주문 아이템을 수정합니다.")
    public ResponseEntity<CommonResponse<OrderItemResponseDto>> updateOrderItem(@AuthenticationPrincipal UserDetails userDetails,
                                                                @PathVariable Long orderItemsId,
                                                                @Valid @RequestBody OrderItemRequestDto dto) {
        User user = UserUtils.getUser(userDetails);
        OrderItemResponseDto update = orderItemService.update(orderItemsId, dto.getQuantity(), user);
        CommonResponse<OrderItemResponseDto> response = new CommonResponse<>(ResponseMessage.ORDER_ITEM_UPDATE_SUCCESS, update);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    //특정유저의 특정 오더 아이템 삭제
    @DeleteMapping("/{orderItemsId}")
    @Operation(summary = "주문 아이템 삭제", description = "특정 주문 아이템을 삭제합니다.")
    public ResponseEntity<CommonResponse<OrderItemResponseDto>> delete(@AuthenticationPrincipal UserDetails userDetails,
                                                       @PathVariable Long orderItemsId) {
        User user = UserUtils.getUser(userDetails);
        OrderItemResponseDto delete = orderItemService.delete(orderItemsId, user);
        CommonResponse<OrderItemResponseDto> response = new CommonResponse<>(ResponseMessage.ORDER_ITEM_DELETE_SUCCESS, delete);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
