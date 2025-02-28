package com.Soo_Shinsa.review.repository;

import com.Soo_Shinsa.global.exception.NotFoundException;
import com.Soo_Shinsa.review.dto.ReviewResponseDto;
import com.Soo_Shinsa.review.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import static com.Soo_Shinsa.global.exception.ErrorCode.NOT_FOUND_REVIEW;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    default Review findByIdOrElseThrow(Long reviewId) {
        return findById(reviewId).orElseThrow(() -> new NotFoundException(NOT_FOUND_REVIEW));
    }

    @Query("SELECT new com.Soo_Shinsa.review.dto.ReviewResponseDto(r.id, r.user.userId, r.product.id, " +
            "r.imageUrl, r.orderItem.id, r.rate, r.content) " +
            "FROM Review r " +
            "WHERE r.product.id = :productId " +
            "AND (:minRate IS NULL OR r.rate >= :minRate) " +
            "AND (:maxRate IS NULL OR r.rate <= :maxRate) " +
            "ORDER BY r.rate DESC")
    Page<ReviewResponseDto> getReviewsAllByProductId(@Param("productId") Long productId,
                                                     @Param("minRate") Integer minRate,
                                                     @Param("maxRate") Integer maxRate,
                                                     Pageable pageable);

    void deleteAllByProductId(Long productId);
}
