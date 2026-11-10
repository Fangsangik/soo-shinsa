package com.Soo_Shinsa.review.controller;

import com.Soo_Shinsa.auth.UserDetailsImp;
import com.Soo_Shinsa.review.dto.ReviewRateDto;
import com.Soo_Shinsa.review.dto.ReviewRequestDto;
import com.Soo_Shinsa.review.dto.ReviewResponseDto;
import com.Soo_Shinsa.review.dto.ReviewUpdateDto;
import com.Soo_Shinsa.review.service.ReviewService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

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

    @GetMapping("/{reviewId}")
    public ResponseEntity<CommonResponse<ReviewResponseDto>> getReview(@PathVariable Long reviewId) {

        ReviewResponseDto review = reviewService.getReview(reviewId);
        CommonResponse<ReviewResponseDto> response = new CommonResponse<>(ResponseMessage.REVIEW_SELECT_SUCCESS, review);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

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

    @GetMapping("/products/{productId}")
    public ResponseEntity<CommonResponse<Page<ReviewResponseDto>>> getAllReviewByProductId(@PathVariable Long productId,
                                                                                           @Valid @ModelAttribute ReviewRateDto reviewRateDto,
                                                                                           @RequestParam(defaultValue = "0") int page,
                                                                                           @RequestParam(defaultValue = "10") int size) {
        Page<ReviewResponseDto> reviews = reviewService.getReviewsByProductId(productId, reviewRateDto, page, size);
        CommonResponse<Page<ReviewResponseDto>> response = new CommonResponse<>(ResponseMessage.REVIEW_SELECT_SUCCESS, reviews);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId,
                                             @AuthenticationPrincipal UserDetailsImp userDetails) {
        User user = UserUtils.getUser(userDetails);
        reviewService.delete(reviewId, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}

