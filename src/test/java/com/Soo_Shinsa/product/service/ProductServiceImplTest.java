package com.Soo_Shinsa.product.service;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.constant.ProductStatus;
import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.exception.NoAuthorizedException;
import com.Soo_Shinsa.image.ImageService;
import com.Soo_Shinsa.order.repository.OrderItemRepository;
import com.Soo_Shinsa.product.dto.ProductOptionRequestDto;
import com.Soo_Shinsa.product.dto.ProductOptionResponseDto;
import com.Soo_Shinsa.product.dto.ProductRequestDto;
import com.Soo_Shinsa.product.dto.ProductUpdateDto;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import com.Soo_Shinsa.product.repository.ProductOptionRepository;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.review.repository.ReviewRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.Soo_Shinsa.support.Fixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 입점사 상품/옵션 관리의 소유권 규칙: ADMIN 은 전부, VENDOR 는 자기 브랜드만. */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    @Mock BrandRepository brandRepository;
    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;
    @Mock ImageService imageService;
    @Mock ProductOptionRepository productOptionRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock com.Soo_Shinsa.wish.repository.WishRepository wishRepository;
    @InjectMocks ProductServiceImpl productService;
    @InjectMocks ProductOptionServiceImpl optionService;

    User owner, otherVendor, admin, customer;
    Brand brand;
    Product product;

    @BeforeEach
    void setUp() {
        owner = user(1L, Role.VENDOR);
        otherVendor = user(2L, Role.VENDOR);
        admin = user(3L, Role.ADMIN);
        customer = user(4L, Role.CUSTOMER);
        brand = brand(100L, owner);
        product = product(10L, brand, 10_000);
        lenient().when(productRepository.findByIdOrElseThrow(10L)).thenReturn(product);
        lenient().when(brandRepository.findByIdOrElseThrow(100L)).thenReturn(brand);
        lenient().when(userRepository.findByIdOrElseThrow(any())).thenAnswer(i -> ((Long) i.getArgument(0)) == 1L ? owner : ((Long) i.getArgument(0)) == 2L ? otherVendor : admin);
    }

    @Test
    void 다른_입점사의_상품은_수정_삭제할_수_없다() {
        ProductUpdateDto dto = ProductUpdateDto.builder().name("x").price(BigDecimal.ONE).status(ProductStatus.SOLD_OUT).build();
        assertThatThrownBy(() -> productService.updateProduct(otherVendor, dto, 10L, null)).isInstanceOf(NoAuthorizedException.class);
        assertThatThrownBy(() -> productService.deleteProduct(10L, otherVendor)).isInstanceOf(NoAuthorizedException.class);
        assertThatThrownBy(() -> productService.createProduct(otherVendor, ProductRequestDto.builder().name("n").price(BigDecimal.TEN).brandId(100L).status(ProductStatus.AVAILABLE).build(), 100L, null))
                .isInstanceOf(NoAuthorizedException.class);
        assertThat(product.getName()).isEqualTo("product10");
        verify(productRepository, never()).delete(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void 고객은_상품을_만들_수_없다() {
        when(userRepository.findByIdOrElseThrow(4L)).thenReturn(customer);
        assertThatThrownBy(() -> productService.createProduct(customer, ProductRequestDto.builder().name("n").price(BigDecimal.TEN).brandId(100L).status(ProductStatus.AVAILABLE).build(), 100L, null))
                .isInstanceOf(NoAuthorizedException.class);
    }

    @Test
    void 소유자와_관리자는_수정할_수_있고_이미지가_없으면_기존_URL_을_유지한다() {
        ProductUpdateDto dto = ProductUpdateDto.builder().name("새 이름").price(new BigDecimal("12000")).status(ProductStatus.SOLD_OUT).build();
        ProductUpdateDto r = productService.updateProduct(owner, dto, 10L, null);
        assertThat(r.getName()).isEqualTo("새 이름");
        assertThat(r.getPrice()).isEqualByComparingTo("12000");
        assertThat(r.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);

        productService.updateProduct(admin, ProductUpdateDto.builder().price(new BigDecimal("1")).build(), 10L, null);
        assertThat(product.getPrice()).isEqualByComparingTo("1");
        assertThat(product.getName()).isEqualTo("새 이름"); // null 필드는 건드리지 않음
        verifyNoInteractions(imageService);
    }

    @Test
    void 삭제는_리뷰_주문항목_옵션을_먼저_지운다() {
        productService.deleteProduct(10L, owner);
        InOrder in = inOrder(reviewRepository, orderItemRepository, productOptionRepository, productRepository);
        in.verify(reviewRepository).deleteAllByProductId(10L);
        in.verify(orderItemRepository).deleteAllByProductId(10L);
        in.verify(productOptionRepository).deleteAllByProductId(10L);
        in.verify(productRepository).delete(product);
        verify(wishRepository).deleteAllByProductId(10L);
    }

    @Test
    void 옵션_생성과_수정도_브랜드_소유권을_따른다() {
        when(productOptionRepository.save(any(ProductOption.class))).thenAnswer(i -> withId(i.getArgument(0), "id", 1L));
        ProductOptionRequestDto req = new ProductOptionRequestDto("L", "White", ProductStatus.SOLD_OUT);

        assertThatThrownBy(() -> optionService.createOption(otherVendor, req, 10L)).isInstanceOf(NoAuthorizedException.class);
        ProductOptionResponseDto created = optionService.createOption(owner, req, 10L);
        assertThat(created.getStatus()).isEqualTo(ProductStatus.SOLD_OUT); // 하드코딩 AVAILABLE 버그 수정 확인
        assertThat(created.getProduct()).isEqualTo(10L);

        ProductOption existing = option(5L, product);
        when(productOptionRepository.findByIdOrElseThrow(5L)).thenReturn(existing);
        assertThatThrownBy(() -> optionService.updateOption(otherVendor, new com.Soo_Shinsa.product.dto.ProductOptionUpdateDto("S", "Red", ProductStatus.AVAILABLE), 5L)).isInstanceOf(NoAuthorizedException.class);
        assertThat(optionService.updateOption(admin, new com.Soo_Shinsa.product.dto.ProductOptionUpdateDto("S", "Red", ProductStatus.SOLD_OUT), 5L).getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }
}
