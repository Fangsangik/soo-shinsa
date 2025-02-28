package com.Soo_Shinsa.category.controller;

import com.Soo_Shinsa.category.dto.SubCategoryRequestDto;
import com.Soo_Shinsa.category.dto.SubCategoryResponseDto;
import com.Soo_Shinsa.category.dto.SubCategoryUpdateRequestDto;
import com.Soo_Shinsa.category.dto.SubCategoryUpdateResponseDto;
import com.Soo_Shinsa.category.service.SubCategoryService;
import com.Soo_Shinsa.global.utils.CommonResponse;
import com.Soo_Shinsa.global.utils.UserUtils;
import com.Soo_Shinsa.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sub-categories")
@RequiredArgsConstructor
@Tag(name = "SubCategory API", description = "서브 카테고리 관련 API")
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    @PostMapping
    @Operation(summary = "서브 카테고리 생성", description = "새로운 서브 카테고리를 생성합니다.")
    public ResponseEntity<CommonResponse<SubCategoryResponseDto>> createSubCategory(@RequestBody SubCategoryRequestDto requestDto,
                                                                                   @AuthenticationPrincipal UserDetails userDetails) {
        User user = UserUtils.getUser(userDetails);
        SubCategoryResponseDto responseDto = subCategoryService.createSubCategory(user, requestDto);
        return ResponseEntity.ok(new CommonResponse<>("서브 카테고리가 성공적으로 생성되었습니다.", responseDto));
    }

    @GetMapping("/{subCategoryId}")
    @Operation(summary = "서브 카테고리 조회", description = "특정 서브 카테고리를 조회합니다.")
    public ResponseEntity<CommonResponse<SubCategoryResponseDto>> findSubCategoryById(@PathVariable Long subCategoryId) {
        SubCategoryResponseDto responseDto = subCategoryService.findSubCategoryById(subCategoryId);
        return ResponseEntity.ok(new CommonResponse<>("서브 카테고리가 조회 되었습니다.", responseDto));
    }

    @PatchMapping("/{subCategoryId}")
    @Operation(summary = "서브 카테고리 수정", description = "서브 카테고리를 수정합니다.")
    public ResponseEntity<CommonResponse<SubCategoryUpdateResponseDto>> updateSubCategory(@RequestBody SubCategoryUpdateRequestDto requestDto,
                                                                                          @PathVariable Long subCategoryId,
                                                                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = UserUtils.getUser(userDetails);
        SubCategoryUpdateResponseDto responseDto = subCategoryService.updateSubCategory(user, requestDto, subCategoryId);
        return ResponseEntity.ok(new CommonResponse<>("서브 카테고리가 성공적으로 수정되었습니다.", responseDto));
    }
 }
