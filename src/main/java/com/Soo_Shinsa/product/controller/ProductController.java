package com.Soo_Shinsa.product.controller;

import com.Soo_Shinsa.product.dto.*;
import com.Soo_Shinsa.product.service.ProductService;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping("/brands/{brandId}")
    public ResponseEntity<CommonResponse<ProductResponseDto>> createProduct(@AuthenticationPrincipal UserDetails userDetails,
                                                                           @Valid @RequestPart ProductRequestDto productRequestDto,
                                                                           @RequestPart(required = false) MultipartFile imageFile,
                                                                           @PathVariable Long brandId) {

        User user = UserUtils.getUser(userDetails);
        ProductResponseDto product = productService.createProduct(user, productRequestDto, brandId, imageFile);
        CommonResponse<ProductResponseDto> response = new CommonResponse<>(ResponseMessage.PRODUCT_CREATE_SUCCESS, product);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<CommonResponse<ProductUpdateDto>> updateProduct(@AuthenticationPrincipal UserDetails userDetails,
                                                          @RequestPart ProductUpdateDto productUpdateDto,
                                                          @RequestPart(required = false) MultipartFile imageFile,
                                                          @PathVariable Long productId) {
        User user = UserUtils.getUser(userDetails);
        ProductUpdateDto productResponseDto = productService.updateProduct(user, productUpdateDto, productId, imageFile);
        CommonResponse<ProductUpdateDto> response = new CommonResponse<>(ResponseMessage.PRODUCT_UPDATE_SUCCESS, productResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<CommonResponse<FindProductResponseDto>> findProduct(@PathVariable Long productId) {
        FindProductResponseDto productResponseDto = productService.findProduct(productId);
        CommonResponse<FindProductResponseDto> response =  new CommonResponse<>(ResponseMessage.PRODUCT_SELECT_SUCCESS, productResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 전체 상품 검색 (브랜드 무관) */
    @GetMapping
    public ResponseEntity<CommonResponse<Page<ProductResponseDto>>> searchProducts(@RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "12") int size,
                                                                              @ModelAttribute FindProductRequestDto requestDto) {
        Page<ProductResponseDto> result = productService.searchProducts(requestDto, page, size);
        return ResponseEntity.ok(new CommonResponse<>(ResponseMessage.PRODUCT_SELECT_SUCCESS, result));
    }

    @GetMapping("/brands/{brandId}")
    public ResponseEntity<CommonResponse<Page<ProductResponseDto>>> findAllProductList(@RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "10") int size,
                                                                       @ModelAttribute FindProductRequestDto requestDto,
                                                                       @PathVariable Long brandId) {
        Page<ProductResponseDto> productResponseDto = productService.findAllProduct(brandId, requestDto, page, size);
        CommonResponse<Page<ProductResponseDto>> response = new CommonResponse<>(ResponseMessage.PRODUCT_SELECT_SUCCESS, productResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@AuthenticationPrincipal UserDetails userDetails,
                                              @PathVariable Long productId) {
        User user = UserUtils.getUser(userDetails);
        productService.deleteProduct(productId, user);
        return ResponseEntity.noContent().build();
    }
}
