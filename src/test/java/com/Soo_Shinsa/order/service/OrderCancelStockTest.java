package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.category.model.Category;
import com.Soo_Shinsa.category.model.SubCategory;
import com.Soo_Shinsa.category.repository.CategoryRepository;
import com.Soo_Shinsa.category.repository.SubCategoryRepository;
import com.Soo_Shinsa.global.constant.BrandStatus;
import com.Soo_Shinsa.global.constant.OrdersStatus;
import com.Soo_Shinsa.global.constant.ProductStatus;
import com.Soo_Shinsa.global.constant.Role;
import com.Soo_Shinsa.global.constant.UserStatus;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.Soo_Shinsa.order.dto.OrdersResponseDto;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import com.Soo_Shinsa.product.repository.ProductOptionRepository;
import com.Soo_Shinsa.product.repository.ProductRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 주문 취소 시 재고가 돌아오는지 검증.
 * 부분 취소만 복원하고 전체 취소는 복원하지 않아 재고가 사라지고 있었다.
 */
@SpringBootTest(properties = "app.order.pending-timeout=PT0S")
class OrderCancelStockTest {

    private static final int STOCK = 5;

    @Autowired private OrdersService ordersService;
    @Autowired private OrdersRepository ordersRepository;
    @Autowired private ProductOptionRepository productOptionRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SubCategoryRepository subCategoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbc;

    private User user;
    private ProductOption option;

    @BeforeEach
    void setUp() {
        TestDataCleaner.clean(jdbc);
        user = userRepository.save(User.builder().email("test@test.com").password("p").name("취소 테스트")
                .phoneNum("01000000000").role(Role.CUSTOMER).status(UserStatus.ACTIVE).build());
        Category category = categoryRepository.save(Category.builder().name("취소테스트").build());
        SubCategory sub = subCategoryRepository.save(
                SubCategory.builder().name("취소테스트").category(category).build());
        Brand brand = brandRepository.save(Brand.builder().user(user).name("취소테스트")
                .registrationNum("000-00-00000").subCategory(sub).status(BrandStatus.OPEN)
                .isCouponLimited(false).build());
        Product product = productRepository.save(Product.builder().name("취소테스트 상품")
                .price(BigDecimal.valueOf(1000)).productStatus(ProductStatus.AVAILABLE).brand(brand).build());
        option = productOptionRepository.save(ProductOption.builder().product(product)
                .color("c").size("s").productStatus(ProductStatus.AVAILABLE).quantity(STOCK).build());
    }

    @AfterEach
    void tearDown() {
        TestDataCleaner.clean(jdbc);
    }

    private int stock() {
        return productOptionRepository.findByIdOrElseThrow(option.getId()).getQuantity();
    }

    @Test
    void 전체_취소하면_재고가_돌아온다() throws Exception {
        OrdersResponseDto order = ordersService.createSingleProductOrder(user, option.getId(), 2);
        assertEquals(STOCK - 2, stock(), "주문 시 재고가 차감된다");

        ordersService.cancelOrder(user, order.getId());

        assertEquals(STOCK, stock(), "취소하면 재고가 복원되어야 한다");
        assertEquals(OrdersStatus.ORDERCANCEL,
                ordersRepository.findByIdOrElseThrow(order.getId()).getStatus());
    }

    @Test
    void 이미_취소된_주문은_다시_취소되지_않는다() throws Exception {
        OrdersResponseDto order = ordersService.createSingleProductOrder(user, option.getId(), 2);
        ordersService.cancelOrder(user, order.getId());

        // 두 번 취소되면 재고가 두 번 복원돼 원래보다 늘어난다
        assertThrows(InvalidInputException.class, () -> ordersService.cancelOrder(user, order.getId()));
        assertEquals(STOCK, stock(), "재고가 중복 복원되면 안 된다");
    }

    @Test
    void 결제되지_않고_방치된_주문은_재고가_반환된다() throws Exception {
        ordersService.createSingleProductOrder(user, option.getId(), 3);
        assertEquals(STOCK - 3, stock());

        // pending-timeout 을 0으로 두었으므로 방금 만든 주문도 만료 대상이다
        int reverted = ordersService.expirePendingOrders(100);

        assertEquals(1, reverted);
        assertEquals(STOCK, stock(), "미결제 주문의 재고는 되돌아와야 한다");
    }

    @Test
    void 이미_취소된_주문은_만료_처리로_재고가_또_늘지_않는다() throws Exception {
        OrdersResponseDto order = ordersService.createSingleProductOrder(user, option.getId(), 3);
        ordersService.cancelOrder(user, order.getId());
        assertEquals(STOCK, stock());

        assertEquals(0, ordersService.expirePendingOrders(100), "취소된 주문은 만료 대상이 아니다");
        assertEquals(STOCK, stock());
    }
}
