package com.Soo_Shinsa.product.controller;

import com.Soo_Shinsa.global.utils.CommonResponse;
import com.Soo_Shinsa.global.utils.ResponseMessage;
import com.Soo_Shinsa.global.utils.UserUtils;
import com.Soo_Shinsa.product.dto.FindProductOptionRequestDto;
import com.Soo_Shinsa.product.dto.ProductOptionRequestDto;
import com.Soo_Shinsa.product.dto.ProductOptionResponseDto;
import com.Soo_Shinsa.product.dto.ProductOptionUpdateDto;
import com.Soo_Shinsa.product.service.ProductOptionService;
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
@RequestMapping("/options")
@Tag(name = "Product Option API", description = "상품 옵션 관련 API")
public class ProductOptionController {

    private final ProductOptionService productOptionService;

    @PostMapping("/products/{productId}")
    @Operation(summary = "상품 옵션 생성", description = "특정 상품의 옵션을 생성합니다.")
    public ResponseEntity<CommonResponse<ProductOptionResponseDto>> createOption(@AuthenticationPrincipal UserDetails userDetails,
                                                                                @Valid @RequestBody ProductOptionRequestDto productOptionRequestDto,
                                                                                @PathVariable Long productId) {
        User user = UserUtils.getUser(userDetails);
        ProductOptionResponseDto productOptionResponseDto = productOptionService.createOption(user, productOptionRequestDto, productId);
        CommonResponse<ProductOptionResponseDto> response = new CommonResponse<>(ResponseMessage.OPTION_CREATE_SUCCESS, productOptionResponseDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{productOptionId}")
    @Operation(summary = "상품 옵션 수정", description = "특정 상품의 옵션을 수정합니다.")
    public ResponseEntity<CommonResponse<ProductOptionResponseDto>> updateOption(@AuthenticationPrincipal UserDetails userDetails,
                                                                 @RequestBody ProductOptionUpdateDto updateDto,
                                                                 @PathVariable Long productOptionId) {
        User user = UserUtils.getUser(userDetails);
        ProductOptionResponseDto productOptionResponseDto = productOptionService.updateOption(user, updateDto, productOptionId);
        CommonResponse<ProductOptionResponseDto> response = new CommonResponse<>(ResponseMessage.OPTION_UPDATE_SUCCESS, productOptionResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{productOptionId}")
    @Operation(summary = "상품 옵션 조회", description = "특정 상품 옵션을 조회합니다.")
    public ResponseEntity<CommonResponse<ProductOptionResponseDto>> findOption(@PathVariable Long productOptionId) {
        ProductOptionResponseDto productOptionResponseDto = productOptionService.findOption(productOptionId);
        CommonResponse<ProductOptionResponseDto> response = new CommonResponse<>(ResponseMessage.OPTION_SELECT_SUCCESS, productOptionResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    @Operation(summary = "상품 옵션 리스트 조회", description = "옵션 조건을 기반으로 상품 리스트를 조회합니다.")
    public ResponseEntity<CommonResponse<Page<ProductOptionResponseDto>>> findOptionListByProductId(@RequestBody FindProductOptionRequestDto requestDto,
                                                                                    @RequestParam(defaultValue = "0") int page,
                                                                                    @RequestParam(defaultValue = "10") int size) {
        Page<ProductOptionResponseDto> productOptionResponseDto = productOptionService.findProductsByOptionalSizeAndColor(requestDto, page, size);
        CommonResponse<Page<ProductOptionResponseDto>> response = new CommonResponse<>(ResponseMessage.OPTION_SELECT_SUCCESS, productOptionResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
