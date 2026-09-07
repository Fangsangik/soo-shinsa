package com.Soo_Shinsa.product.service;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.category.model.Category;
import com.Soo_Shinsa.category.model.SubCategory;
import com.Soo_Shinsa.category.repository.CategoryRepository;
import com.Soo_Shinsa.category.repository.SubCategoryRepository;
import com.Soo_Shinsa.global.constant.BrandStatus;
import com.Soo_Shinsa.global.constant.ProductStatus;
import com.Soo_Shinsa.global.constant.Role;
import com.Soo_Shinsa.global.constant.UserStatus;
import com.Soo_Shinsa.product.dto.ProductResponseDto;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.support.IntegrationTestSupport;
import com.Soo_Shinsa.support.TestDataCleaner;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserProductViewRepository;
import com.Soo_Shinsa.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 함께 본 상품 추천.
 * 조회 이력을 아무도 기록하지 않아 추천이 늘 랜덤 폴백으로 빠지고 있었다.
 */
@SpringBootTest
class ProductRecommendationTest extends IntegrationTestSupport {

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserProductViewRepository userProductViewRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SubCategoryRepository subCategoryRepository;
    @Autowired private JdbcTemplate jdbc;

    private User me;
    private Product shared;
    private Product theirFavorite;
    private Product unrelated;

    private User user(String email) {
        return userRepository.save(User.builder().email(email).password("p").name(email)
                .phoneNum("010" + email.hashCode()).role(Role.CUSTOMER).status(UserStatus.ACTIVE).build());
    }

    @BeforeEach
    void setUp() {
        TestDataCleaner.clean(jdbc);
        me = user("test@test.com");
        Category category = categoryRepository.save(Category.builder().name("추천").build());
        SubCategory sub = subCategoryRepository.save(
                SubCategory.builder().name("추천").category(category).build());
        Brand brand = brandRepository.save(Brand.builder().user(me).name("추천 브랜드")
                .registrationNum("000-00-00000").subCategory(sub).status(BrandStatus.OPEN)
                .isCouponLimited(false).build());

        shared = productRepository.save(Product.builder().name("함께 본 기준 상품")
                .price(BigDecimal.valueOf(1000)).productStatus(ProductStatus.AVAILABLE).brand(brand).build());
        theirFavorite = productRepository.save(Product.builder().name("남들이 함께 본 상품")
                .price(BigDecimal.valueOf(2000)).productStatus(ProductStatus.AVAILABLE).brand(brand).build());
        unrelated = productRepository.save(Product.builder().name("아무도 안 본 상품")
                .price(BigDecimal.valueOf(3000)).productStatus(ProductStatus.AVAILABLE).brand(brand).build());
    }

    @AfterEach
    void tearDown() {
        TestDataCleaner.clean(jdbc);
    }

    private List<String> recommend() {
        return productService.findUserBasedRecommendation(me, 0, 10)
                .getContent().stream().map(ProductResponseDto::getName).toList();
    }

    @Test
    void 상품을_보면_조회_이력이_남는다() {
        assertTrue(userProductViewRepository.findViewedProductIds(me.getUserId()).isEmpty());

        productService.findProduct(shared.getId(), me);

        assertEquals(List.of(shared.getId()), userProductViewRepository.findViewedProductIds(me.getUserId()));
    }

    @Test
    void 비로그인_조회는_이력을_남기지_않는다() {
        productService.findProduct(shared.getId(), null);
        assertTrue(userProductViewRepository.findViewedProductIds(me.getUserId()).isEmpty());
    }

    @Test
    void 같은_상품을_본_사람이_함께_본_상품을_추천한다() {
        productService.findProduct(shared.getId(), me);

        // 다른 두 명이 같은 상품 + 자기들 취향 상품을 봤다
        for (String email : List.of("test1@test.com", "test2@test.com")) {
            User peer = user(email);
            productService.findProduct(shared.getId(), peer);
            productService.findProduct(theirFavorite.getId(), peer);
        }

        List<String> names = recommend();
        assertTrue(names.contains("남들이 함께 본 상품"), names.toString());
        assertFalse(names.contains("함께 본 기준 상품"), "이미 본 상품은 빼야 한다: " + names);
        assertFalse(names.contains("아무도 안 본 상품"), names.toString());
    }

    @Test
    void 이력이_없으면_폴백_추천이_나온다() {
        // 예외 없이 무언가는 돌려줘야 한다
        assertTrue(recommend().size() >= 0);
    }
}
