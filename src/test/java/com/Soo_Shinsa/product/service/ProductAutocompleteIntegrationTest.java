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
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.support.IntegrationTestSupport;
import com.Soo_Shinsa.support.TestDataCleaner;
import com.Soo_Shinsa.user.model.User;
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
 * 자동완성이 FULLTEXT 인덱스 위에서 실제로 동작하는지 확인한다.
 * 네이티브 MATCH ... AGAINST 라 단위 테스트로는 검증되지 않는다.
 */
@SpringBootTest
class ProductAutocompleteIntegrationTest extends IntegrationTestSupport {

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SubCategoryRepository subCategoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        TestDataCleaner.clean(jdbc);
        User owner = userRepository.save(User.builder().email("test@test.com").password("p")
                .name("자동완성").phoneNum("01000000000").role(Role.VENDOR).status(UserStatus.ACTIVE).build());
        Category category = categoryRepository.save(Category.builder().name("자동완성").build());
        SubCategory sub = subCategoryRepository.save(
                SubCategory.builder().name("자동완성").category(category).build());
        Brand brand = brandRepository.save(Brand.builder().user(owner).name("자동완성 브랜드")
                .registrationNum("000-00-00000").subCategory(sub).status(BrandStatus.OPEN)
                .isCouponLimited(false).build());

        for (String name : List.of("옥스포드 셔츠", "링클프리 셔츠", "코튼 반팔 티셔츠",
                                   "경량 패딩 점퍼", "와이드 데님 팬츠")) {
            productRepository.save(Product.builder().name(name).price(BigDecimal.valueOf(10000))
                    .productStatus(ProductStatus.AVAILABLE).brand(brand).build());
        }
    }

    @AfterEach
    void tearDown() {
        TestDataCleaner.clean(jdbc);
    }

    @Test
    void FULLTEXT_인덱스가_만들어져_있다() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                "WHERE table_schema = DATABASE() AND table_name = 'product' AND index_name = ?",
                Integer.class, "ft_product_name");
        assertEquals(1, count, "V1 마이그레이션이 만들어야 할 인덱스가 없다");
    }

    @Test
    void 부분_일치로_추천된다() {
        List<String> shirts = productService.autocomplete("셔츠", 8);
        assertTrue(shirts.contains("옥스포드 셔츠"), shirts.toString());
        assertTrue(shirts.contains("링클프리 셔츠"), shirts.toString());
        assertTrue(shirts.contains("코튼 반팔 티셔츠"), "단어 중간에 있어도 잡혀야 한다: " + shirts);
        assertFalse(shirts.contains("경량 패딩 점퍼"), shirts.toString());
    }

    @Test
    void 앞에서_일치하는_것이_먼저_온다() {
        List<String> shirts = productService.autocomplete("셔츠", 8);
        assertTrue(shirts.indexOf("옥스포드 셔츠") < shirts.indexOf("코튼 반팔 티셔츠"), shirts.toString());
    }

    @Test
    void BOOLEAN_MODE_연산자가_들어와도_깨지지_않는다() {
        // + - * " ( ) 등은 BOOLEAN MODE 연산자라 그대로 넘기면 구문 오류가 난다
        for (String bad : List.of("셔츠+", "-셔츠", "셔츠\"", "셔츠(", "*셔츠*", "셔츠 ~")) {
            productService.autocomplete(bad, 8);
        }
    }

    @Test
    void 결과가_없으면_빈_목록이다() {
        assertTrue(productService.autocomplete("존재하지않는상품명", 8).isEmpty());
    }
}
