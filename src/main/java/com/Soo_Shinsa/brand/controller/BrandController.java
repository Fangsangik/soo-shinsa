package com.Soo_Shinsa.brand.controller;

import com.Soo_Shinsa.brand.dto.*;
import com.Soo_Shinsa.brand.service.BrandService;
import com.Soo_Shinsa.global.utils.CommonResponse;
import com.Soo_Shinsa.global.utils.ResponseMessage;
import com.Soo_Shinsa.global.utils.UserUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/brands")
@Tag(name = "Brand API", description = "브랜드 관련 API")
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    @Operation(summary = "브랜드 생성", description = "새로운 브랜드를 생성합니다.")
    public ResponseEntity<CommonResponse<BrandResponseDto>> createBrand(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BrandRequestDto brandRequestDto
    ) {
        BrandResponseDto brandResponseDto = brandService.create(UserUtils.getUser(userDetails), brandRequestDto);
        CommonResponse<BrandResponseDto> response = new CommonResponse<>(ResponseMessage.BRAND_CREATE_SUCCESS, brandResponseDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{brandId}")
    @Operation(summary = "브랜드 수정", description = "브랜드 정보를 수정합니다.")
    public ResponseEntity<CommonResponse<BrandUpdateResponseDto>> updateBrand(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BrandUpdateRequestDto brandRequestDto,
            @PathVariable Long brandId
    ) {
        BrandUpdateResponseDto brandRefuseResponseDto = brandService.update(
                UserUtils.getUser(userDetails),
                brandRequestDto,
                brandId);
        CommonResponse<BrandUpdateResponseDto> response = new CommonResponse<>(ResponseMessage.BRAND_UPDATE_SUCCESS, brandRefuseResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{brandId}")
    @Operation(summary = "브랜드 조회", description = "특정 브랜드 정보를 조회합니다.")
    public ResponseEntity<CommonResponse<BrandResponseDto>> getBrand(@PathVariable Long brandId) {
        BrandResponseDto findBrand = brandService.findBrandById(brandId);
        CommonResponse<BrandResponseDto> response = new CommonResponse<>(ResponseMessage.BRAND_SELECT_SUCCESS, findBrand);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/vendor")
    @Operation(summary = "사용자 브랜드 조회", description = "사용자가 소유한 브랜드를 조회합니다.")
    public ResponseEntity<CommonResponse<List<BrandResponseDto>>> getAllBrandByUserId(@AuthenticationPrincipal UserDetails userDetails) {
        List<BrandResponseDto> getAllBrand = brandService.getAllByUserId(UserUtils.getUser(userDetails));
        CommonResponse<List<BrandResponseDto>> response = new CommonResponse<>(ResponseMessage.BRAND_SELECT_SUCCESS, getAllBrand);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    @Operation(summary = "전체 브랜드 조회", description = "등록된 모든 브랜드를 페이징하여 조회합니다.")
    public ResponseEntity<CommonResponse<Page<FindBrandAllResponseDto>>> getAllBrands(@RequestParam(defaultValue = "0") int page,
                                                                                      @RequestParam(defaultValue = "10") int size) {
        Page<FindBrandAllResponseDto> getAllBrand = brandService.getAll(page, size);
        CommonResponse<Page<FindBrandAllResponseDto>> response = new CommonResponse<>(ResponseMessage.BRAND_SELECT_SUCCESS, getAllBrand);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ========== Admin 전용 엔드포인트들 ==========
    
    @PatchMapping("/admin/{brandId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "브랜드 승인", description = "관리자가 브랜드 입점을 승인합니다.")
    public ResponseEntity<CommonResponse<BrandResponseDto>> approveBrand(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long brandId,
            @Valid @RequestBody BrandApprovalDto approvalDto
    ) {
        BrandResponseDto approvedBrand = brandService.approveBrand(
            UserUtils.getUser(userDetails), 
            brandId, 
            approvalDto
        );
        CommonResponse<BrandResponseDto> response = new CommonResponse<>("브랜드 승인에 성공했습니다.", approvedBrand);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/admin/{brandId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "브랜드 거절", description = "관리자가 브랜드 입점을 거절합니다.")
    public ResponseEntity<CommonResponse<BrandResponseDto>> rejectBrand(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long brandId,
            @Valid @RequestBody BrandRejectionDto rejectionDto
    ) {
        BrandResponseDto rejectedBrand = brandService.rejectBrand(
            UserUtils.getUser(userDetails), 
            brandId, 
            rejectionDto
        );
        CommonResponse<BrandResponseDto> response = new CommonResponse<>("브랜드 거절에 성공했습니다.", rejectedBrand);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "승인 대기 브랜드 조회", description = "승인 대기 중인 브랜드 목록을 조회합니다.")
    public ResponseEntity<CommonResponse<Page<BrandResponseDto>>> getPendingBrands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<BrandResponseDto> pendingBrands = brandService.getPendingBrands(page, size);
        CommonResponse<Page<BrandResponseDto>> response = new CommonResponse<>("승인 대기 브랜드 조회에 성공했습니다.", pendingBrands);
        return ResponseEntity.ok(response);
    }
}
