package com.Soo_Shinsa.coupon.service;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.category.model.Category;
import com.Soo_Shinsa.category.model.SubCategory;
import com.Soo_Shinsa.category.repository.CategoryRepository;
import com.Soo_Shinsa.category.repository.SubCategoryRepository;
import com.Soo_Shinsa.coupon.dto.CouponBrandRelationDto;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponBrandRelation;
import com.Soo_Shinsa.coupon.repository.CouponBrandRelationRepository;
import com.Soo_Shinsa.coupon.repository.CouponRepository;
import com.Soo_Shinsa.coupon.repository.CouponUserRepository;
import com.Soo_Shinsa.global.constant.Role;
import com.Soo_Shinsa.global.exception.NotFoundException;
import com.Soo_Shinsa.global.constant.UserStatus;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.Soo_Shinsa.support.IntegrationTestSupport;
import com.Soo_Shinsa.support.TestDataCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SpringBootTest
class CouponServiceImplTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CouponServiceImpl couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponStockGuard couponStockGuard;

    @Autowired
    private CouponUserRepository couponUserRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubCategoryRepository subCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponBrandRelationRepository couponBrandRelationRepository;

    private Coupon coupon;
    private User testUser;
    private Brand brand;
    private SubCategory subCategory;
    private Category category;

    @AfterEach
    void tearDown() {
        // 실행이 끝나면 자기 픽스처는 DB 에 남기지 않는다
        TestDataCleaner.clean(jdbcTemplate);
    }

    @BeforeEach
    void setUp() {
        TestDataCleaner.clean(jdbcTemplate);
        testUser = User.builder()
                .email("test@test.com")
                .password("password")
                .name("테스트 유저")
                .phoneNum("01012345678")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(testUser);

        category = Category.builder()
                .name("테스트 카테고리")
                .build();
        categoryRepository.save(category);

        subCategory = SubCategory.builder()
                .name("테스트 서브 카테고리")
                .category(category)
                .build();
        subCategoryRepository.save(subCategory);


        brand = Brand.builder()
                .user(testUser)
                .name("테스트 브랜드")
                .registrationNum("123456789")
                .isCouponLimited(false)
                .couponCount(100)
                .subCategory(subCategory)
                .build();
        brandRepository.save(brand);

        coupon = Coupon.builder()
                .couponName("테스트 쿠폰")
                .discountRate(BigDecimal.TEN)
                .maxCount(10)
                .build();
        couponRepository.save(coupon);

        CouponBrandRelation couponBrandRelation = CouponBrandRelation.builder()
                .coupon(coupon) // 저장된 쿠폰과 연관
                .brand(brand) // 브랜드와 연관
                .build();
        coupon.getCouponBrandRelations().add(couponBrandRelation); // 관계 추가
        couponBrandRelationRepository.save(couponBrandRelation);
        couponStockGuard.reset(coupon.getId());

    }

    @Test
    void 병렬_쿠폰_발급_테스트() throws InterruptedException {
        int threadCount = 2; // 동시에 실행할 요청 수
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    couponService.issue(coupon.getId(), testUser);
                    System.out.println("쿠폰 발급 완료: " + testUser.getUserId());
                } catch (Exception e) {
                    System.err.println("에러 발생: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 완료될 때까지 대기
        executorService.shutdown();

        // 결과 검증
        Coupon issued = couponRepository.findByIdOrElseThrow(coupon.getId());
        long issuedCoupons = couponUserRepository.countByCouponId(coupon.getId());

        log.info("발급된 쿠폰 수: {}", issuedCoupons);
        assertEquals(1, issuedCoupons); // maxCount가 10이므로 발급된 쿠폰 수는 10이어야 함
        assertEquals(1, issued.getIssuedCount()); // issuedCount도 10이어야 함
    }

    //5000건 정도 넣으니 테스트가 도중에 안돌아감
    @Test
    void 병렬_쿠폰_발급_테스트_선착순() throws InterruptedException {
        int threadCount = 100; // 동시에 실행할 요청 수
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 사용자 ID 기반으로 정렬된 사용자 리스트 생성
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= threadCount; i++) {
            User user = User.builder()
                    .email("test" + i + "@test.com")
                    .password("password")
                    .name("테스트 유저 " + i)
                    .phoneNum("010123456" + i)
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
            users.add(user);
        }

        // 사용자별로 쿠폰 발급 시도
        for (User user : users) {
            executorService.submit(() -> {
                try {
                    couponService.issue(coupon.getId(), user);
                    log.info("쿠폰 발급 완료: {}", user.getUserId());
                } catch (Exception e) {
                    log.error("에러 발생: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS); // 10초 동안 기다리도록 설정
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS); // 10초 후 강제 종료


        // 결과 검증
        Coupon issued = couponRepository.findByIdOrElseThrow(coupon.getId());
        long issuedCoupons = couponUserRepository.countByCouponId(coupon.getId());

        log.info("발급된 쿠폰 수: {}", issuedCoupons);
        assertEquals(10, issuedCoupons); // maxCount가 10이므로 발급된 쿠폰 수는 10이어야 함
        assertEquals(10, issued.getIssuedCount()); // issuedCount도 10이어야 함
    }

    @Test
    void 없는_쿠폰은_발급할_수_없다() {
        // 예전에는 여기서 쿠폰을 새로 만들어버렸다
        long before = couponRepository.count();
        assertThrows(NotFoundException.class, () -> couponService.issue(99_999_999L, testUser));
        assertEquals(before, couponRepository.count(), "발급 실패가 쿠폰을 만들면 안 된다");
    }

    @Test
    void 여러명이_발급받아도_브랜드_관계는_늘지_않는다() throws InterruptedException {
        long before = couponBrandRelationRepository.count();

        int threadCount = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            User user = userRepository.save(User.builder()
                    .email("test" + (500 + i) + "@test.com").password("p").name("u" + i)
                    .phoneNum("0105" + i).role(Role.ADMIN).status(UserStatus.ACTIVE).build());
            pool.submit(() -> {
                try {
                    couponService.issue(coupon.getId(), user);
                } catch (Exception ignored) {
                    // 정원 초과는 정상 동작
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        // 발급마다 relation 을 만들던 버그가 있었다
        assertEquals(before, couponBrandRelationRepository.count());
    }
}
