package com.Soo_Shinsa.review.service;

import com.Soo_Shinsa.constant.OrdersStatus;
import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.exception.NoAuthorizedException;
import com.Soo_Shinsa.image.ImageService;
import com.Soo_Shinsa.order.model.OrderItem;
import com.Soo_Shinsa.order.repository.OrderItemRepository;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.review.dto.ReviewRequestDto;
import com.Soo_Shinsa.review.dto.ReviewUpdateDto;
import com.Soo_Shinsa.review.model.Review;
import com.Soo_Shinsa.review.repository.ReviewRepository;
import com.Soo_Shinsa.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;

import static com.Soo_Shinsa.support.Fixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 리뷰 수정/삭제는 작성자만, 작성은 주문 당사자만. (프론트: 상품 페이지의 "내 리뷰" 수정/삭제 버튼) */
@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {
    @Mock ReviewRepository reviewRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock ImageService imageService;
    @Mock ProductRepository productRepository;
    @Mock RedissonClient redissonClient;
    @InjectMocks ReviewServiceImpl service;

    User me, other;
    Product product;
    Review review;

    @BeforeEach
    void setUp() {
        me = user(1L, Role.CUSTOMER); other = user(2L, Role.CUSTOMER);
        product = product(10L, brand(100L, user(3L, Role.VENDOR)), 10_000);
        review = withId(Review.builder().rate(3).content("보통").imageUrl("https://img/old.png").product(product).user(me).build(), "id", 7L);
        lenient().when(reviewRepository.findByIdOrElseThrow(7L)).thenReturn(review);
    }

    @Test
    void 남의_주문_항목에는_리뷰를_쓸_수_없다() {
        OrderItem item = orderItem(5L, order(1L, other, OrdersStatus.DELIVERYCOMPLETED), product, 1, BigDecimal.TEN);
        when(orderItemRepository.findByIdOrElseThrow(5L)).thenReturn(item);
        assertThatThrownBy(() -> service.createReview(5L, new ReviewRequestDto(5, "굿", 10L, null), me, null)).isInstanceOf(NoAuthorizedException.class);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void 작성자가_아니면_수정_삭제_불가() {
        assertThatThrownBy(() -> service.updateReview(7L, ReviewUpdateDto.builder().rate(1).content("x").build(), other, null)).isInstanceOf(NoAuthorizedException.class);
        assertThatThrownBy(() -> service.delete(7L, other)).isInstanceOf(NoAuthorizedException.class);
        assertThat(review.getRate()).isEqualTo(3);
        verify(reviewRepository, never()).delete(any());
        verifyNoInteractions(imageService);
    }

    @Test
    void 작성자_수정은_null_필드를_건드리지_않고_이미지가_없으면_기존_URL_유지() {
        ReviewUpdateDto r = service.updateReview(7L, ReviewUpdateDto.builder().rate(5).build(), me, null);
        assertThat(r.getRate()).isEqualTo(5);
        assertThat(review.getContent()).isEqualTo("보통");
        assertThat(review.getImageUrl()).isEqualTo("https://img/old.png");
        verifyNoInteractions(imageService);
    }

    @Test
    void 작성자_삭제는_이미지도_함께_지운다() {
        service.delete(7L, me);
        verify(imageService).deleteImage("https://img/old.png");
        verify(reviewRepository).delete(review);
    }
}
