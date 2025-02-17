package com.Soo_Shinsa.coupon.service;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.category.model.Category;
import com.Soo_Shinsa.category.model.SubCategory;
import com.Soo_Shinsa.category.repository.CategoryRepository;
import com.Soo_Shinsa.category.repository.SubCategoryRepository;
import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.constant.UserStatus;
import com.Soo_Shinsa.coupon.dto.CouponBrandRelationDto;
import com.Soo_Shinsa.coupon.dto.CouponRequestDto;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponBrandRelation;
import com.Soo_Shinsa.coupon.repository.CouponRepository;
import com.Soo_Shinsa.coupon.repository.CouponUserRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CouponServiceImplTest {

    @Autowired
    private CouponServiceImpl couponService;

    @Autowired
    private CouponRepository couponRepository;

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

    private Coupon coupon;
    private User testUser;
    private CouponRequestDto couponRequestDto;
    private Brand brand;
    private SubCategory subCategory;
    private Category category;

    @BeforeEach
    void setUp() {
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
                .couponName("병렬 처리 테스트 쿠폰")
                .discountRate(new BigDecimal("10.0"))
                .maxCount(10)
                .build();

        couponRepository.save(coupon);

        CouponBrandRelation couponBrandRelation = CouponBrandRelation.builder()
                .coupon(coupon) // 저장된 쿠폰과 연관
                .brand(brand) // 브랜드와 연관
                .build();
        coupon.getCouponBrandRelations().add(couponBrandRelation); // 관계 추가
        couponRepository.save(coupon);

        couponRequestDto = CouponRequestDto.builder()
                .couponId(coupon.getId())
                .couponName(coupon.getCouponName())
                .discountRate(coupon.getDiscountRate())
                .maxCount(coupon.getMaxCount())
                .brands(Collections.singletonList(new CouponBrandRelationDto(brand.getId())))
                .build();

    }

    @Test
    void 병렬_쿠폰_발급_테스트() throws InterruptedException {
        int threadCount = 100; // 동시에 실행할 요청 수
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    couponService.createCoupon(couponRequestDto, testUser);
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
        Coupon coupon = couponRepository.findByIdOrElseThrow(couponRequestDto.getCouponId());
        long issuedCoupons = couponUserRepository.count();

        System.out.println("발급된 쿠폰 수: " + issuedCoupons);
        assertEquals(1, issuedCoupons); // maxCount가 10이므로 발급된 쿠폰 수는 10이어야 함
        assertEquals(1, coupon.getIssuedCount()); // issuedCount도 10이어야 함
    }

    //5000건 정도 넣으니 테스트가 도중에 안돌아감
    @Test
    void 병렬_쿠폰_발급_테스트_선착순() throws InterruptedException {
        int threadCount = 4000; // 동시에 실행할 요청 수
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
                    couponService.createCoupon(couponRequestDto, user);
                    System.out.println("쿠폰 발급 완료: " + user.getUserId());
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
        Coupon coupon = couponRepository.findByIdOrElseThrow(couponRequestDto.getCouponId());
        long issuedCoupons = couponUserRepository.count();

        System.out.println("발급된 쿠폰 수: " + issuedCoupons);
        assertEquals(10, issuedCoupons); // maxCount가 10이므로 발급된 쿠폰 수는 10이어야 함
        assertEquals(10, coupon.getIssuedCount()); // issuedCount도 10이어야 함
    }
}