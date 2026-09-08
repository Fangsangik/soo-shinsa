package com.Soo_Shinsa.user.service;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.category.model.Category;
import com.Soo_Shinsa.category.model.SubCategory;
import com.Soo_Shinsa.category.repository.CategoryRepository;
import com.Soo_Shinsa.category.repository.SubCategoryRepository;
import com.Soo_Shinsa.global.constant.BrandStatus;
import com.Soo_Shinsa.global.constant.GradeType;
import com.Soo_Shinsa.global.constant.OrdersStatus;
import com.Soo_Shinsa.global.constant.ProductStatus;
import com.Soo_Shinsa.global.constant.Role;
import com.Soo_Shinsa.global.constant.UserStatus;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.Soo_Shinsa.order.dto.OrdersResponseDto;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import com.Soo_Shinsa.order.service.OrderCancellationService;
import com.Soo_Shinsa.order.service.OrdersService;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import com.Soo_Shinsa.product.repository.ProductOptionRepository;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.support.IntegrationTestSupport;
import com.Soo_Shinsa.support.TestDataCleaner;
import com.Soo_Shinsa.user.model.Grade;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.model.UserGrade;
import com.Soo_Shinsa.user.repository.GradeRepository;
import com.Soo_Shinsa.user.repository.UserGradeRepository;
import com.Soo_Shinsa.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 포인트 사용 → 결제 승인 적립/승급 → 취소 롤백까지의 전체 흐름.
 */
@SpringBootTest
class PointFlowTest extends IntegrationTestSupport {

    @Autowired private OrdersService ordersService;
    @Autowired private OrderCancellationService orderCancellationService;
    @Autowired private PointService pointService;
    @Autowired private UserRepository userRepository;
    @Autowired private GradeRepository gradeRepository;
    @Autowired private UserGradeRepository userGradeRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SubCategoryRepository subCategoryRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductOptionRepository productOptionRepository;
    @Autowired private OrdersRepository ordersRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate tx;

    private User customer;
    private ProductOption option;

    @BeforeEach
    void setUp() {
        Grade rookie = gradeRepository.findByName(GradeType.ROOKIE)
                .orElseGet(() -> gradeRepository.save(new Grade(GradeType.ROOKIE, new BigDecimal("0.01"), new BigDecimal("1000"))));
        gradeRepository.findByName(GradeType.GOLD)
                .orElseGet(() -> gradeRepository.save(new Grade(GradeType.GOLD, new BigDecimal("0.05"), new BigDecimal("5000"))));

        UserGrade userGrade = userGradeRepository.save(new UserGrade(rookie));
        customer = userRepository.save(User.builder()
                .email("test1@test.com").password("pw").name("포인트고객").phoneNum("010-0000-0001")
                .status(UserStatus.ACTIVE).role(Role.CUSTOMER).userGrade(userGrade).build());

        User vendor = userRepository.save(User.builder()
                .email("test2@test.com").password("pw").name("포인트업주").phoneNum("010-0000-0002")
                .status(UserStatus.ACTIVE).role(Role.VENDOR).build());

        Category category = categoryRepository.save(Category.builder().name("포인트카테고리").build());
        SubCategory sub = subCategoryRepository.save(SubCategory.builder().category(category).name("포인트서브").build());
        Brand brand = brandRepository.save(Brand.builder()
                .name("포인트브랜드").context("t").registrationNum("111-11-11111")
                .status(BrandStatus.OPEN).subCategory(sub).user(vendor).build());
        Product product = productRepository.save(Product.builder()
                .name("포인트상품").price(new BigDecimal("10000")).productStatus(ProductStatus.AVAILABLE).brand(brand).build());
        option = productOptionRepository.save(ProductOption.builder()
                .size("M").color("B").quantity(100).productStatus(ProductStatus.AVAILABLE).product(product).build());
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM wish WHERE user_id IN (SELECT user_id FROM `user` WHERE email LIKE 'test%@test.com')");
        TestDataCleaner.clean(jdbc);
    }

    @Test
    void 잔액보다_많은_포인트는_쓸_수_없다() {
        assertThrows(InvalidInputException.class,
                () -> ordersService.createSingleProductOrder(customer, option.getId(), 1, new BigDecimal("1")));
    }

    @Test
    void 사용_적립_승급_취소까지_흐른다() {
        // 잔액 5000 지급
        tx.executeWithoutResult(s -> {
            User u = userRepository.findByIdOrElseThrow(customer.getUserId());
            u.addPoint(new BigDecimal("5000"));
        });

        // 1) 3000 포인트 쓰고 주문 -> 결제 금액 7000, 잔액 2000
        OrdersResponseDto created = ordersService.createSingleProductOrder(
                customer, option.getId(), 1, new BigDecimal("3000"));
        assertEquals(0, new BigDecimal("7000").compareTo(created.getTotalPrice()));
        assertEquals(0, new BigDecimal("2000").compareTo(reload().getPoint()));

        // 2) 결제 승인 흉내: 적립(7000×1%=70) + 누적 구매 7000 -> GOLD(5000) 승급
        tx.executeWithoutResult(s -> {
            Orders order = ordersRepository.findByIdOrElseThrow(created.getId());
            order.updateStatus(OrdersStatus.ORDERCOMPLETED);
            pointService.settlePurchase(order);
        });
        User afterSettle = reload();
        assertEquals(0, new BigDecimal("2070").compareTo(afterSettle.getPoint()));
        assertEquals(0, new BigDecimal("7000").compareTo(afterSettle.getTotalPurchase()));
        assertEquals(GradeType.GOLD, afterSettle.getUserGrade().getGrade().getName());

        // 3) 취소: 사용 3000 반환, 적립 70 회수 -> 잔액 5000
        boolean cancelled = orderCancellationService.cancel(created.getId(), "포인트 테스트");
        assertTrue(cancelled);
        User afterCancel = reload();
        assertEquals(0, new BigDecimal("5000").compareTo(afterCancel.getPoint()));
        assertEquals(0, BigDecimal.ZERO.compareTo(afterCancel.getTotalPurchase()));
    }

    private User reload() {
        return tx.execute(s -> {
            User u = userRepository.findByIdOrElseThrow(customer.getUserId());
            u.getUserGrade().getGrade().getName(); // LAZY 초기화
            return u;
        });
    }
}
