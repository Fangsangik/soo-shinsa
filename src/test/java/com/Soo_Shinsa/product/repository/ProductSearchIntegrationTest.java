package com.Soo_Shinsa.product.repository;

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
import com.Soo_Shinsa.product.dto.FindProductRequestDto;
import com.Soo_Shinsa.product.dto.ProductResponseDto;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.service.ProductService;
import com.Soo_Shinsa.support.IntegrationTestSupport;
import com.Soo_Shinsa.support.TestDataCleaner;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 통합 검색이 FULLTEXT 함수로 실제 DB 에서 도는지 확인.
 * QueryDSL 이 만든 match_against 가 SQL 로 번역되는지는 통합 테스트로만 검증된다.
 */
@SpringBootTest
class ProductSearchIntegrationTest extends IntegrationTestSupport {

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SubCategoryRepository subCategoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbc;

    private Long brandId;

    @BeforeEach
    void setUp() {
        TestDataCleaner.clean(jdbc);
        User owner = userRepository.save(User.builder().email("test@test.com").password("p")
                .name("검색").phoneNum("01000000000").role(Role.VENDOR).status(UserStatus.ACTIVE).build());
        Category category = categoryRepository.save(Category.builder().name("검색").build());
        SubCategory sub = subCategoryRepository.save(
                SubCategory.builder().name("검색").category(category).build());
        Brand brand = brandRepository.save(Brand.builder().user(owner).name("검색 브랜드")
                .registrationNum("000-00-00000").subCategory(sub).status(BrandStatus.OPEN)
                .isCouponLimited(false).build());
        brandId = brand.getId();

        record Seed(String name, long price) { }
        for (Seed seed : List.of(new Seed("옥스포드 셔츠", 30_000), new Seed("링클프리 셔츠", 50_000),
                                 new Seed("코튼 반팔 티셔츠", 20_000), new Seed("경량 패딩 점퍼", 90_000))) {
            productRepository.save(Product.builder().name(seed.name())
                    .price(BigDecimal.valueOf(seed.price()))
                    .productStatus(ProductStatus.AVAILABLE).brand(brand).build());
        }
    }

    @AfterEach
    void tearDown() {
        TestDataCleaner.clean(jdbc);
    }

    private List<String> search(FindProductRequestDto dto) {
        Page<ProductResponseDto> page = productService.findAllProduct(null, dto, 0, 20);
        return page.getContent().stream().map(ProductResponseDto::getName).toList();
    }

    @Test
    void 키워드로_부분_일치_검색된다() {
        List<String> names = search(new FindProductRequestDto("셔츠", null, null, null, null));
        assertEquals(3, names.size(), names.toString());
        assertTrue(names.contains("코튼 반팔 티셔츠"), "단어 중간도 잡혀야 한다: " + names);
    }

    @Test
    void 키워드와_가격_조건이_함께_적용된다() {
        List<String> names = search(new FindProductRequestDto(
                "셔츠", BigDecimal.valueOf(25_000), BigDecimal.valueOf(40_000), null, null));
        assertEquals(List.of("옥스포드 셔츠"), names);
    }

    @Test
    void 한글자_키워드도_동작한다() {
        // ngram 이 못 잡는 길이라 LIKE 로 떨어진다
        assertEquals(3, search(new FindProductRequestDto("셔", null, null, null, null)).size());
    }

    @Test
    void BOOLEAN_MODE_연산자가_들어와도_깨지지_않는다() {
        for (String bad : List.of("셔츠+", "-셔츠", "셔츠\"", "*셔츠*", "셔츠 ~")) {
            search(new FindProductRequestDto(bad, null, null, null, null));
        }
    }

    @Test
    void 결과가_없으면_빈_페이지다() {
        assertTrue(search(new FindProductRequestDto("존재하지않는상품", null, null, null, null)).isEmpty());
    }
}
