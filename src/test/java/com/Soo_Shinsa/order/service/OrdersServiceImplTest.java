package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.cartitem.dto.ApplyCouponCartRequestDto;
import com.Soo_Shinsa.cartitem.model.CartItem;
import com.Soo_Shinsa.cartitem.repository.CartItemRepository;
import com.Soo_Shinsa.cartitem.service.CartItemService;
import com.Soo_Shinsa.category.model.Category;
import com.Soo_Shinsa.category.model.SubCategory;
import com.Soo_Shinsa.category.repository.CategoryRepository;
import com.Soo_Shinsa.category.repository.SubCategoryRepository;
import com.Soo_Shinsa.constant.*;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponBrandRelation;
import com.Soo_Shinsa.coupon.repository.CouponBrandRelationRepository;
import com.Soo_Shinsa.coupon.repository.CouponRepository;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import com.Soo_Shinsa.product.repository.ProductOptionRepository;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@SpringBootTest
class OrdersServiceImplTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubCategoryRepository subCategoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CouponBrandRelationRepository couponBrandRelationRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrdersService ordersService;

    private User user;
    private Category category;
    private SubCategory subCategory;
    private Brand brand;
    private Brand adidas;
    private Product product;
    private ProductOption productOption;
    private CartItem cartItem;
    private Coupon validCoupon;
    private Coupon expiredCoupon;
    private Coupon wrongBrandCoupon;
    private CouponBrandRelation couponBrandRelation;

    @Autowired
    private CartItemService cartItemService;

    @BeforeEach
    void setUp() {
        log.info("🛠 setUp");

        couponBrandRelationRepository.deleteAll();
        couponRepository.deleteAll();
        cartItemRepository.deleteAll();
        ordersRepository.deleteAll();


        user = User.builder()
                .email("test@test.com")
                .name("Test User")
                .role(Role.CUSTOMER)
                .phoneNum("010-1234-5678")
                .password("password123")
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        category = Category.builder()
                .name("의류")
                .build();
        categoryRepository.save(category);

        subCategory = SubCategory.builder()
                .name("티셔츠")
                .category(category)
                .build();
        subCategoryRepository.save(subCategory);

        brand = Brand.builder()
                .user(user)
                .name("나이키")
                .subCategory(subCategory)
                .isCouponLimited(true)
                .couponCount(5)
                .registrationNum("123-45-67890")
                .status(BrandStatus.OPEN)
                .build();
        brandRepository.save(brand);

        adidas = Brand.builder()
                .user(user)
                .name("아디다스")
                .subCategory(subCategory)
                .isCouponLimited(true)
                .couponCount(5)
                .registrationNum("123-45-67890")
                .status(BrandStatus.OPEN)
                .build();
        brandRepository.save(adidas);

        product = Product.builder()
                .name("나이키 드라이핏 티셔츠")
                .price(BigDecimal.valueOf(50000))
                .brand(brand)
                .productStatus(ProductStatus.AVAILABLE)
                .build();
        productRepository.save(product);

        productOption = ProductOption.builder()
                .product(product)
                .color("화이트")
                .size("L")
                .productStatus(ProductStatus.AVAILABLE)
                .quantity(10)
                .build();
        productOptionRepository.save(productOption);

        cartItem = CartItem.builder()
                .product(product)
                .productOption(productOption)
                .user(user)
                .quantity(1)
                .build();
        cartItemRepository.save(cartItem);

        validCoupon = Coupon.builder()
                .couponName("나이키 10% 할인")
                .couponType(CouponType.SPECIFIC_BRAND)
                .discountRate(BigDecimal.valueOf(10.0))
                .maxCount(10)
                .build();
        couponRepository.save(validCoupon);

        expiredCoupon = Coupon.builder()
                .couponName("만료된 쿠폰")
                .couponType(CouponType.SPECIFIC_BRAND)
                .discountRate(BigDecimal.valueOf(15.0))
                .maxCount(5)
                .build();
        couponRepository.save(expiredCoupon);

        wrongBrandCoupon = Coupon.builder()
                .couponName("아디다스 20% 할인")
                .couponType(CouponType.SPECIFIC_BRAND)
                .discountRate(BigDecimal.valueOf(20.0))
                .maxCount(5)
                .build();
        couponRepository.save(wrongBrandCoupon);

        couponBrandRelation = CouponBrandRelation.builder()
                .coupon(validCoupon)
                .brand(brand)
                .build();
        couponBrandRelationRepository.save(couponBrandRelation);

        CouponBrandRelation wrongBrandRelation = CouponBrandRelation.builder()
                .coupon(wrongBrandCoupon)
                .brand(adidas)
                .build();
        couponBrandRelationRepository.save(wrongBrandRelation);
    }

    @Test
    void testConcurrentStockReduction() throws InterruptedException {
        log.info("상품 재고 동시성 test");

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    ordersService.createSingleProductOrder(user, productOption.getId(), 1);
                    ProductOption updateOption = productOptionRepository.findByIdOrElseThrow(productOption.getId());

                    log.info("주문 성공 : 남은 재고 {}", updateOption.getQuantity());
                } catch (Exception e) {
                    log.error("예외 발생 : {}", e.getMessage());
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();
        executorService.shutdown();

        ProductOption left = productOptionRepository.findByIdOrElseThrow(productOption.getId());
        log.info("남은 재고 : {}", left.getQuantity());

        Assertions.assertEquals(0, left.getQuantity());
    }

    @Test
    void createSingleOrderCartItemWithMultipleUsers() throws InterruptedException {
        int stockQuantity = 10;
        int userCount = stockQuantity; // 유저 수 == 주문 시도 횟수
        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch countDownLatch = new CountDownLatch(userCount);

        List<User> users = new ArrayList<>();
        List<CartItem> cartItems = new ArrayList<>();

        // ✅ 여러 유저 및 카트 아이템 생성 (각 유저가 서로 다른 cartItem을 사용)
        for (int i = 0; i < userCount; i++) {
            User newUser = User.builder()
                    .email("test" + i + "@test.com")
                    .name("Test User " + i)
                    .role(Role.CUSTOMER)
                    .phoneNum("010-1234-567" + i)
                    .password("password123")
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(newUser);
            users.add(newUser);

            CartItem newCartItem = CartItem.builder()
                    .product(product)
                    .productOption(productOption)
                    .user(newUser)
                    .quantity(1)
                    .build();
            cartItemRepository.save(newCartItem);
            cartItems.add(newCartItem);
        }

        // ✅ 여러 유저가 동시에 주문 진행
        for (int i = 0; i < userCount; i++) {
            final User currentUser = users.get(i);
            final CartItem currentCartItem = cartItems.get(i);

            executorService.submit(() -> {
                try {
                    ApplyCouponCartRequestDto requestDto = ApplyCouponCartRequestDto.builder()
                            .couponId(validCoupon.getId())
                            .build();

                    cartItemService.applyCoupon(currentCartItem.getId(), requestDto, currentUser);
                    ordersService.createSingleOrderCartItem(currentUser, currentCartItem.getId());

                    ProductOption updateQuantity = productOptionRepository.findByIdOrElseThrow(currentCartItem.getProductOption().getId());
                    Coupon updatedCoupon = couponRepository.findByIdOrElseThrow(validCoupon.getId());

                    log.info("✅ 주문 성공 - 유저: {}, 남은 재고: {}, 남은 쿠폰 수: {}",
                            currentUser.getEmail(), updateQuantity.getQuantity(), updatedCoupon.getMaxCount());
                } catch (Exception e) {
                    log.error("❌ 예외 발생 - 유저: {}, 에러 메시지: {}", currentUser.getEmail(), e.getMessage());
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();  // 🔥 모든 스레드가 종료될 때까지 대기
        executorService.shutdown();  // 🔥 스레드 풀 종료

        // ✅ 최종 재고 및 쿠폰 개수 검증
        ProductOption left = productOptionRepository.findByIdOrElseThrow(productOption.getId());
        Coupon updatedCoupon = couponRepository.findByIdOrElseThrow(validCoupon.getId());

        log.info("✅ 테스트 완료 - 최종 남은 재고: {}, 최종 남은 쿠폰 수: {}", left.getQuantity(), updatedCoupon.getMaxCount());

        Assertions.assertEquals(0, left.getQuantity());
        Assertions.assertEquals(0, updatedCoupon.getMaxCount());
    }

    @Test
    void applyDifferentCouponsAndCreateOrderConcurrently() throws InterruptedException {
        couponRepository.deleteAll();
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch applyCouponLatch = new CountDownLatch(threadCount);
        CountDownLatch orderLatch = new CountDownLatch(1);

        // ✅ 10개의 서로 다른 쿠폰 생성
        List<Coupon> coupons = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            coupons.add(couponRepository.save(
                    Coupon.builder()
                            .couponName("쿠폰 " + i)
                            .discountRate(BigDecimal.valueOf(15.0))
                            .maxCount(1)  // ✅ 각 쿠폰은 한 번만 사용 가능
                            .build()
            ));

            couponBrandRelationRepository.save(
                    CouponBrandRelation.builder()
                            .coupon(coupons.get(i))
                            .brand(brand)
                            .build()
            );
        }
        Assertions.assertEquals(10, coupons.size(), "🚨 생성된 쿠폰 개수가 10개가 아님! 확인 필요.");
        log.info("✅ 생성된 쿠폰 개수 확인: {}", coupons.size());

        List<CartItem> cartItems = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CartItem cartItem = CartItem.builder()
                    .product(product)
                    .productOption(productOption)
                    .user(user)
                    .quantity(1)
                    .build();
            cartItemRepository.save(cartItem);

            ApplyCouponCartRequestDto requestDto = ApplyCouponCartRequestDto.builder()
                    .couponId(coupons.get(i).getId())
                    .build();

            executorService.submit(() -> {
                try {
                    cartItemService.applyCoupon(cartItem.getId(), requestDto, user);
                    CartItem updatedCartItem = cartItemRepository.findById(cartItem.getId()).orElseThrow();
                    Assertions.assertNotNull(updatedCartItem.getCoupon(), "🚨 쿠폰이 적용되지 않았음!");

                    log.info("✅ 쿠폰 적용 성공 - 카트 아이템 ID: {}", cartItem.getId());
                } catch (Exception e) {
                    log.error("🚨 쿠폰 적용 실패 - {}", e.getMessage());
                } finally {
                    applyCouponLatch.countDown();
                }
            });

            cartItems.add(cartItem);
        }

        // ✅ 쿠폰 적용이 완료될 때까지 대기
        applyCouponLatch.await();

        executorService.submit(() -> {
            try {
                ordersService.createAllOrderFromCart(user);
                log.info("✅ 한 번에 주문 성공 - 유저: {}", user.getEmail());
            } catch (Exception e) {
                log.error("🚨 주문 실패 - 예외 발생!", e);
            } finally {
                orderLatch.countDown();
            }
        });

        // ✅ 주문 생성이 완료될 때까지 대기
        orderLatch.await();
        executorService.shutdown();

        // ✅ 최종 재고 및 쿠폰 개수 검증
        ProductOption left = productOptionRepository.findByIdOrElseThrow(productOption.getId());
        log.info("✅ 테스트 완료 - 최종 남은 재고: {}", left.getQuantity());
        Assertions.assertEquals(0, left.getQuantity());

        for (Coupon coupon : coupons) {
            Coupon updatedCoupons = couponRepository.findByIdOrElseThrow(coupon.getId());
            log.info("🎟 최종 쿠폰 재고 확인 - 쿠폰 ID: {}, 남은 재고: {}", updatedCoupons.getId(), updatedCoupons.getMaxCount());
            Assertions.assertEquals(0, updatedCoupons.getMaxCount(),
                    "🚨 쿠폰 ID: " + updatedCoupons.getId() + "의 재고가 예상과 다름!");
        }
    }
}