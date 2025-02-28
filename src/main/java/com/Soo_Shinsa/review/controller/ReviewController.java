package com.Soo_Shinsa.review.controller;

import com.Soo_Shinsa.global.auth.UserDetailsImp;
import com.Soo_Shinsa.global.utils.CommonResponse;
import com.Soo_Shinsa.global.utils.ResponseMessage;
import com.Soo_Shinsa.global.utils.UserUtils;
import com.Soo_Shinsa.review.dto.ReviewRateDto;
import com.Soo_Shinsa.review.dto.ReviewRequestDto;
import com.Soo_Shinsa.review.dto.ReviewResponseDto;
import com.Soo_Shinsa.review.dto.ReviewUpdateDto;
import com.Soo_Shinsa.review.service.ReviewService;
import com.Soo_Shinsa.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "리뷰 API", description = "리뷰 관련 기능을 제공합니다.")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 생성", description = "주문한 상품에 대한 리뷰를 작성합니다.")
    @PostMapping("/order-item/{orderItemId}")
    public ResponseEntity<CommonResponse<ReviewResponseDto>> createReview(@PathVariable Long orderItemId,
                                                                          @Valid @RequestPart ReviewRequestDto requestDto,
                                                                          @RequestPart(required = false) MultipartFile imageFile,
                                                                          @AuthenticationPrincipal UserDetailsImp userDetails) {
        User user = UserUtils.getUser(userDetails);
        ReviewResponseDto review = reviewService.createReview(orderItemId, requestDto, user, imageFile);
        CommonResponse<ReviewResponseDto> response = new CommonResponse<>(ResponseMessage.REVIEW_CREATE_SUCCESS, review);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "리뷰 상세 조회", description = "특정 리뷰의 상세 정보를 가져옵니다.")
    @GetMapping("/{reviewId}")
    public ResponseEntity<CommonResponse<ReviewResponseDto>> getReview(@PathVariable Long reviewId) {

        ReviewResponseDto review = reviewService.getReview(reviewId);
        CommonResponse<ReviewResponseDto> response = new CommonResponse<>(ResponseMessage.REVIEW_SELECT_SUCCESS, review);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "리뷰 수정", description = "작성한 리뷰를 수정합니다.")
    @PatchMapping("/{reviewId}")
    public ResponseEntity<CommonResponse<ReviewUpdateDto>> updateReview(@PathVariable Long reviewId,
                                                                        @Valid @RequestPart ReviewUpdateDto updateDto,
                                                                        @RequestPart(required = false) MultipartFile imageFile,
                                                                        @AuthenticationPrincipal UserDetailsImp userDetails) {
        User user = UserUtils.getUser(userDetails);
        ReviewUpdateDto review = reviewService.updateReview(reviewId, updateDto, user, imageFile);
        CommonResponse<ReviewUpdateDto> response = new CommonResponse<>(ResponseMessage.REVIEW_UPDATE_SUCCESS, review);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "상품별 리뷰 조회", description = "특정 상품의 리뷰 목록을 조회합니다.")
    @GetMapping("/products/{productId}")
    public ResponseEntity<CommonResponse<Page<ReviewResponseDto>>> getAllReviewByProductId(@PathVariable Long productId,
                                                                                           @Valid @RequestBody(required = false) ReviewRateDto reviewRateDto,
                                                                                           @RequestParam(defaultValue = "0") int page,
                                                                                           @RequestParam(defaultValue = "10") int size) {
        Page<ReviewResponseDto> reviews = reviewService.getReviewsByProductId(productId, reviewRateDto, page, size);
        CommonResponse<Page<ReviewResponseDto>> response = new CommonResponse<>(ResponseMessage.REVIEW_SELECT_SUCCESS, reviews);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "리뷰 삭제", description = "특정 리뷰를 삭제합니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId,
                                             @AuthenticationPrincipal UserDetailsImp userDetails) {
        User user = UserUtils.getUser(userDetails);
        reviewService.delete(reviewId, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}

