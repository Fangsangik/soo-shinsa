package com.Soo_Shinsa.product.controller;

import com.Soo_Shinsa.global.utils.CommonResponse;
import com.Soo_Shinsa.global.utils.ResponseMessage;
import com.Soo_Shinsa.global.utils.UserUtils;
import com.Soo_Shinsa.product.dto.*;
import com.Soo_Shinsa.product.service.ProductService;
import com.Soo_Shinsa.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
@Tag(name = "Product API", description = "상품 관련 API")
public class ProductController {

    private final ProductService productService;

    /**
     * 이미지를 같이 올릴 때(multipart).
     * multipart 만 받게 해 두는 바람에 JSON 으로 부르면 500(MultipartException)이 났다.
     * 이미지는 선택 항목이므로 아래 JSON 전용 핸들러를 따로 둔다.
     */
    @PostMapping(value = "/brands/{brandId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "상품 생성(이미지 포함)", description = "이미지와 함께 상품을 생성합니다.")
    public ResponseEntity<CommonResponse<ProductResponseDto>> createProduct(@AuthenticationPrincipal UserDetails userDetails,
                                                                            @Valid @RequestPart ProductRequestDto productRequestDto,
                                                                            @RequestPart(required = false) MultipartFile imageFile,
                                                                            @PathVariable Long brandId) {
        return created(userDetails, productRequestDto, brandId, imageFile);
    }

    @PostMapping(value = "/brands/{brandId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "상품 생성", description = "이미지 없이 상품을 생성합니다.")
    public ResponseEntity<CommonResponse<ProductResponseDto>> createProductWithoutImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProductRequestDto productRequestDto,
            @PathVariable Long brandId) {
        return created(userDetails, productRequestDto, brandId, null);
    }

    private ResponseEntity<CommonResponse<ProductResponseDto>> created(UserDetails userDetails,
                                                                      ProductRequestDto dto,
                                                                      Long brandId,
                                                                      MultipartFile imageFile) {
        User user = UserUtils.getUser(userDetails);
        ProductResponseDto product = productService.createProduct(user, dto, brandId, imageFile);
        CommonResponse<ProductResponseDto> response = new CommonResponse<>(ResponseMessage.PRODUCT_CREATE_SUCCESS, product);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "상품 수정(이미지 포함)", description = "이미지와 함께 상품을 수정합니다.")
    public ResponseEntity<CommonResponse<ProductUpdateDto>> updateProduct(@AuthenticationPrincipal UserDetails userDetails,
                                                                          @RequestPart ProductUpdateDto productUpdateDto,
                                                                          @RequestPart(required = false) MultipartFile imageFile,
                                                                          @PathVariable Long productId) {
        return updated(userDetails, productUpdateDto, productId, imageFile);
    }

    @PatchMapping(value = "/{productId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "상품 수정", description = "이미지 없이 상품을 수정합니다.")
    public ResponseEntity<CommonResponse<ProductUpdateDto>> updateProductWithoutImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ProductUpdateDto productUpdateDto,
            @PathVariable Long productId) {
        return updated(userDetails, productUpdateDto, productId, null);
    }

    private ResponseEntity<CommonResponse<ProductUpdateDto>> updated(UserDetails userDetails,
                                                                    ProductUpdateDto productUpdateDto,
                                                                    Long productId,
                                                                    MultipartFile imageFile) {
        User user = UserUtils.getUser(userDetails);
        ProductUpdateDto productResponseDto = productService.updateProduct(user, productUpdateDto, productId, imageFile);
        CommonResponse<ProductUpdateDto> response = new CommonResponse<>(ResponseMessage.PRODUCT_UPDATE_SUCCESS, productResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "상품 조회", description = "특정 상품을 조회합니다.")
    public ResponseEntity<CommonResponse<FindProductResponseDto>> findProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        // 비로그인도 볼 수 있는 화면이라 userDetails 가 null 일 수 있다
        User viewer = userDetails == null ? null : UserUtils.getUser(userDetails);
        FindProductResponseDto productResponseDto = productService.findProduct(productId, viewer);
        CommonResponse<FindProductResponseDto> response = new CommonResponse<>(ResponseMessage.PRODUCT_SELECT_SUCCESS, productResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/autocomplete")
    @Operation(summary = "상품명 자동완성", description = "입력한 키워드가 포함된 상품명을 추천합니다. 2글자 이상부터 동작합니다.")
    public ResponseEntity<CommonResponse<List<String>>> autocomplete(@RequestParam String keyword,
                                                                     @RequestParam(defaultValue = "8") int limit) {
        CommonResponse<List<String>> response =
                new CommonResponse<>(ResponseMessage.PRODUCT_SELECT_SUCCESS, productService.autocomplete(keyword, limit));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/search/popular")
    @Operation(summary = "인기 검색어", description = "오늘 많이 찾은 검색어를 반환합니다.")
    public ResponseEntity<CommonResponse<List<String>>> popularKeywords(
            @RequestParam(defaultValue = "10") int limit) {
        CommonResponse<List<String>> response =
                new CommonResponse<>(ResponseMessage.PRODUCT_SELECT_SUCCESS, productService.popularKeywords(limit));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/search")
    @Operation(summary = "상품 통합 검색", description = "브랜드 구분 없이 상품명/가격/카테고리/판매상태로 검색합니다.")
    public ResponseEntity<CommonResponse<Page<ProductResponseDto>>> searchProducts(@RequestParam(defaultValue = "0") int page,
                                                                                   @RequestParam(defaultValue = "10") int size,
                                                                                   @ModelAttribute FindProductRequestDto requestDto) {
        Page<ProductResponseDto> productResponseDto = productService.findAllProduct(null, requestDto, page, size);
        CommonResponse<Page<ProductResponseDto>> response = new CommonResponse<>(ResponseMessage.PRODUCT_SELECT_SUCCESS, productResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/brands/{brandId}")
    @Operation(summary = "브랜드별 상품 리스트 조회", description = "브랜드 ID로 해당 브랜드의 상품을 조회합니다.")
    public ResponseEntity<CommonResponse<Page<ProductResponseDto>>> findAllProductList(@RequestParam(defaultValue = "0") int page,
                                                                                       @RequestParam(defaultValue = "10") int size,
                                                                                       @ModelAttribute FindProductRequestDto requestDto,
                                                                                       @PathVariable Long brandId) {
        Page<ProductResponseDto> productResponseDto = productService.findAllProduct(brandId, requestDto, page, size);
        CommonResponse<Page<ProductResponseDto>> response = new CommonResponse<>(ResponseMessage.PRODUCT_SELECT_SUCCESS, productResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/recommendations")
    @Operation(summary = "사용자 기반 추천 상품 리스트 조회", description = "사용자 기반 추천 상품 리스트를 조회합니다.")
    public ResponseEntity<CommonResponse<Page<ProductResponseDto>>> findUserBasedRecommendation(@AuthenticationPrincipal UserDetails userDetails,
                                                                                               @RequestParam(defaultValue = "0") int page,
                                                                                               @RequestParam(defaultValue = "10") int size) {
        User user = UserUtils.getUser(userDetails);
        Page<ProductResponseDto> productResponseDto = productService.findUserBasedRecommendation(user, page, size);
        CommonResponse<Page<ProductResponseDto>> response = new CommonResponse<>(ResponseMessage.PRODUCT_SELECT_SUCCESS, productResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "상품 삭제", description = "특정 상품을 삭제합니다.")
    public ResponseEntity<Void> deleteProduct(@AuthenticationPrincipal UserDetails userDetails,
                                              @PathVariable Long productId) {
        User user = UserUtils.getUser(userDetails);
        productService.deleteProduct(productId, user);
        return ResponseEntity.noContent().build();
    }
}
