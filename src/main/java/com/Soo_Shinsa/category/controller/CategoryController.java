package com.Soo_Shinsa.category.controller;

import com.Soo_Shinsa.category.dto.CategoryRequestDto;
import com.Soo_Shinsa.category.dto.CategoryResponseDto;
import com.Soo_Shinsa.category.dto.CategoryUpdateRequestDto;
import com.Soo_Shinsa.category.dto.FindCategoryResponseDto;
import com.Soo_Shinsa.category.service.CategoryService;
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
@RequestMapping("/categories")
@Tag(name = "Category API", description = "카테고리 관련 API")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "카테고리 생성", description = "새로운 카테고리를 생성합니다.")
    public ResponseEntity<CommonResponse<CategoryResponseDto>> create(@AuthenticationPrincipal UserDetails userDetails,
                                                                     @Valid @RequestBody CategoryRequestDto dto
    ) {
        User user = UserUtils.getUser(userDetails);
        CategoryResponseDto saved = categoryService.create(user, dto);

        CommonResponse<CategoryResponseDto> response = new CommonResponse<>(ResponseMessage.CATEGORY_CREATE_SUCCESS,saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "카테고리 조회", description = "특정 카테고리를 조회합니다.")
    public ResponseEntity<CommonResponse<CategoryResponseDto>> findById(@PathVariable Long categoryId) {
        CategoryResponseDto findCategory = categoryService.findById(categoryId);
        CommonResponse<CategoryResponseDto> response = new CommonResponse<>(ResponseMessage.CATEGORY_SELECT_SUCCESS,findCategory);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    @Operation(summary = "전체 카테고리 조회", description = "모든 카테고리를 페이징하여 조회합니다.")
    public ResponseEntity<CommonResponse<Page<FindCategoryResponseDto>>> findAll(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size) {
        Page<FindCategoryResponseDto> findAll = categoryService.findAll(page, size);
        CommonResponse<Page<FindCategoryResponseDto>> response = new CommonResponse<>(ResponseMessage.CATEGORY_SELECT_SUCCESS,findAll);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{categoryId}")
    @Operation(summary = "카테고리 수정", description = "카테고리를 수정합니다.")
    public ResponseEntity<CommonResponse<CategoryResponseDto>> update(@AuthenticationPrincipal UserDetails userDetails,
                                                      @Valid @RequestBody CategoryUpdateRequestDto dto,
                                                      @PathVariable Long categoryId
    ) {
        User user = UserUtils.getUser(userDetails);
        CategoryResponseDto update = categoryService.update(user, dto, categoryId);
        CommonResponse<CategoryResponseDto> response = new CommonResponse<>(ResponseMessage.CATEGORY_UPDATE_SUCCESS,update);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
