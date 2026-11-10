package com.Soo_Shinsa.order.controller;

import com.Soo_Shinsa.order.dto.OrderDateRequestDto;
import com.Soo_Shinsa.order.dto.OrderItemRequestDto;
import com.Soo_Shinsa.order.dto.OrderItemResponseDto;
import com.Soo_Shinsa.order.service.OrderItemService;
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
@RequestMapping("/orderitems")
public class OrderItemController {
    private final OrderItemService orderItemService;

    //오더 아이템 생성
    @PostMapping
    public ResponseEntity<CommonResponse<OrderItemResponseDto>> createOrderItem(@Valid @RequestBody OrderItemRequestDto requestDto,
                                                                               @AuthenticationPrincipal UserDetails userDetails) {
        User user = UserUtils.getUser(userDetails);
        OrderItemResponseDto orderItem = orderItemService.createOrderItem(requestDto, user);
        CommonResponse<OrderItemResponseDto> response = new CommonResponse<>(ResponseMessage.ORDER_ITEM_CREATE_SUCCESS, orderItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //특정유저의 특정 오더아이템 읽기
    @GetMapping("/{OrderItemsId}")
    public ResponseEntity<CommonResponse<OrderItemResponseDto>> findById(@PathVariable Long OrderItemsId,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = UserUtils.getUser(userDetails);
        OrderItemResponseDto findOrder = orderItemService.findById(OrderItemsId, user);
        CommonResponse<OrderItemResponseDto> response = new CommonResponse<>(ResponseMessage.ORDER_ITEM_SELECT_SUCCESS, findOrder);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    //특정 유저의 모든 오더아이템들을 읽기
    @GetMapping
    public ResponseEntity<CommonResponse<Page<OrderItemResponseDto>>> readOrderItem(@AuthenticationPrincipal UserDetails userDetails,
                                                                    @ModelAttribute OrderDateRequestDto dateRequestDto,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        User user = UserUtils.getUser(userDetails);
        Page<OrderItemResponseDto> byAll = orderItemService.findByAll(user, dateRequestDto, page, size);
        CommonResponse<Page<OrderItemResponseDto>> response = new CommonResponse<>(ResponseMessage.ORDER_ITEM_SELECT_SUCCESS, byAll);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    //특정 유저의 특정 오더아이템 수정
    @PatchMapping("/{orderItemsId}")
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
    public ResponseEntity<CommonResponse<OrderItemResponseDto>> delete(@AuthenticationPrincipal UserDetails userDetails,
                                                       @PathVariable Long orderItemsId) {
        User user = UserUtils.getUser(userDetails);
        OrderItemResponseDto delete = orderItemService.delete(orderItemsId, user);
        CommonResponse<OrderItemResponseDto> response = new CommonResponse<>(ResponseMessage.ORDER_ITEM_DELETE_SUCCESS, delete);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
