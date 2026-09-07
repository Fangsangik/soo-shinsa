package com.Soo_Shinsa.coupon.service;

import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponBrandRelation;
import com.Soo_Shinsa.coupon.repository.CouponBrandRelationRepository;
import com.Soo_Shinsa.coupon.repository.CouponRepository;
import com.Soo_Shinsa.coupon.repository.CouponUserRepository;
import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.category.model.Category;
import com.Soo_Shinsa.category.model.SubCategory;
import com.Soo_Shinsa.category.repository.CategoryRepository;
import com.Soo_Shinsa.category.repository.SubCategoryRepository;
import com.Soo_Shinsa.global.constant.BrandStatus;
import com.Soo_Shinsa.global.constant.Role;
import com.Soo_Shinsa.global.constant.UserStatus;
import com.Soo_Shinsa.support.TestDataCleaner;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 선차단을 끈 비교군. 모든 요청이 DB 까지 간다.
 *
 * 큐 도입 여부를 판단하기 위한 근거를 남긴다.
 * 정원 대비 요청이 훨씬 많을 때, 몇 건이 DB 까지 가고 몇 건이 앞단에서 끊기는지 본다.
 */
@Slf4j
@SpringBootTest(properties = "app.coupon.prefilter-enabled=false")
class CouponIssueLoadNoPrefilterTest {

    private static final int CAPACITY = 10;
    private static final int REQUESTS = 2000;

    @Autowired private CouponServiceImpl couponService;
    @Autowired private CouponStockGuard couponStockGuard;
    @Autowired private CouponRepository couponRepository;
    @Autowired private CouponBrandRelationRepository couponBrandRelationRepository;
    @Autowired private CouponUserRepository couponUserRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SubCategoryRepository subCategoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private List<User> users;
    private Long couponId;

    @BeforeEach
    void setUp() {
        TestDataCleaner.clean(jdbcTemplate);

        User owner = userRepository.save(User.builder()
                .email("test@test.com").password("p").name("부하 테스트 업주")
                .phoneNum("01000000000").role(Role.ADMIN).status(UserStatus.ACTIVE).build());

        Category category = categoryRepository.save(Category.builder().name("부하테스트").build());
        SubCategory subCategory = subCategoryRepository.save(
                SubCategory.builder().name("부하테스트").category(category).build());
        Brand brand = brandRepository.save(Brand.builder()
                .user(owner).name("부하테스트 브랜드").registrationNum("000-00-00000")
                .subCategory(subCategory).status(BrandStatus.OPEN).isCouponLimited(false).build());

        Coupon coupon = couponRepository.save(Coupon.builder()
                .couponName("선착순 부하 테스트").discountRate(BigDecimal.TEN).maxCount(CAPACITY).build());
        couponId = coupon.getId();
        couponBrandRelationRepository.save(CouponBrandRelation.builder()
                .coupon(coupon).brand(brand).build());
        couponStockGuard.reset(couponId);


        users = new ArrayList<>();
        for (int i = 0; i < REQUESTS; i++) {
            users.add(userRepository.save(User.builder()
                    .email("test" + i + "@test.com").password("p").name("u" + i)
                    .phoneNum("010" + i).role(Role.ADMIN).status(UserStatus.ACTIVE).build()));
        }
    }

    @AfterEach
    void tearDown() {
        couponStockGuard.reset(couponId);
        TestDataCleaner.clean(jdbcTemplate);
    }

    @Test
    void 정원_10에_요청_2000() throws InterruptedException {
        log.info("### 선차단 OFF (비교군) ###");
        ExecutorService pool = Executors.newFixedThreadPool(64);
        CountDownLatch latch = new CountDownLatch(REQUESTS);
        AtomicInteger issued = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        long start = System.nanoTime();
        for (User user : users) {
            pool.submit(() -> {
                try {
                    couponService.issue(couponId, user);
                    issued.incrementAndGet();
                } catch (Exception e) {
                    rejected.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        assertTrue(latch.await(120, TimeUnit.SECONDS), "120초 내에 끝나야 한다");
        pool.shutdown();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        long issuedInDb = couponUserRepository.countByCouponId(couponId);
        Coupon after = couponRepository.findByIdOrElseThrow(couponId);

        log.info("=== 선착순 부하 측정 ===");
        log.info("정원 {} / 요청 {}", CAPACITY, REQUESTS);
        log.info("성공 {} / 거절 {}", issued.get(), rejected.get());
        log.info("DB issued_count {} / coupon_user {}", after.getIssuedCount(), issuedInDb);
        log.info("소요 {} ms  →  {} req/s", elapsedMs, REQUESTS * 1000L / Math.max(elapsedMs, 1));

        // 부하가 아무리 몰려도 정원은 정확해야 한다
        assertEquals(CAPACITY, issued.get());
        assertEquals(CAPACITY, issuedInDb);
        assertEquals(CAPACITY, after.getIssuedCount());
    }
}
