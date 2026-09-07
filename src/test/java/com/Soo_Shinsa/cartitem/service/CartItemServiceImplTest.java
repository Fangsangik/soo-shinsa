package com.Soo_Shinsa.cartitem.service;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.cartitem.dto.ApplyCouponCartRequestDto;
import com.Soo_Shinsa.cartitem.dto.ApplyCouponCartResponseDto;
import com.Soo_Shinsa.cartitem.model.CartItem;
import com.Soo_Shinsa.cartitem.repository.CartItemRepository;
import com.Soo_Shinsa.category.model.Category;
import com.Soo_Shinsa.category.model.SubCategory;
import com.Soo_Shinsa.category.repository.CategoryRepository;
import com.Soo_Shinsa.category.repository.SubCategoryRepository;
import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponBrandRelation;
import com.Soo_Shinsa.coupon.repository.CouponBrandRelationRepository;
import com.Soo_Shinsa.coupon.repository.CouponRepository;
import com.Soo_Shinsa.global.constant.*;
import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import com.Soo_Shinsa.product.repository.ProductOptionRepository;
import com.Soo_Shinsa.product.repository.ProductRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
class CartItemServiceImplTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    private com.Soo_Shinsa.coupon.repository.CouponUserRepository couponUserRepository;

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

    @AfterEach
    void tearDown() {
        // 실행이 끝나면 자기 픽스처는 DB 에 남기지 않는다
        TestDataCleaner.clean(jdbcTemplate);
    }

    @BeforeEach
    void setUp() {
        log.info("🛠 setUp");
        TestDataCleaner.clean(jdbcTemplate);
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
                .build();
        productRepository.save(product);

        productOption = ProductOption.builder()
                .product(product)
                .color("화이트")
                .size("L")
                .productStatus(ProductStatus.AVAILABLE)
                .quantity(100)
                .build();
        productOptionRepository.save(productOption);

        // cartItem 필드가 선언만 되어 있고 만들어지지 않아 테스트가 NPE 로 죽었다
        cartItem = CartItem.builder()
                .product(product)
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

    @Transactional
    @Test
    void applyCouponToCart() {
        log.info("🚀 applyCouponToCart");
        // given
        ApplyCouponCartRequestDto requestDto = ApplyCouponCartRequestDto.builder()
                .couponId(validCoupon.getId())
                .build();
        log.info("✅ 쿠폰 적용 성공 : {}", requestDto);

        // when
        ApplyCouponCartResponseDto response = cartItemService.applyCoupon(cartItem.getId(), requestDto, user);
        log.info("✅ 쿠폰 적용 결과 : {}", response);

        assertEquals(BigDecimal.valueOf(4500000.0), response.getDiscountedPrice());
        log.info("✅ 쿠폰 적용 결과 : {}", response.getDiscountedPrice());
    }

    @Transactional
    @Test
    void inValidCoupon() {
        log.info("🚨 inValidCoupon 테스트 시작");

        // given
        ApplyCouponCartRequestDto requestDto = ApplyCouponCartRequestDto.builder()
                .couponId(wrongBrandCoupon.getId())
                .build();
        log.info("✅ 잘못된 쿠폰 DTO 생성 : {}", requestDto);

        CouponBrandRelation relation = couponBrandRelationRepository.findByCoupon(wrongBrandCoupon);
        log.info("🎫 쿠폰 ID: {}", wrongBrandCoupon.getId());
        log.info("💡 쿠폰이 적용된 브랜드 ID: {}", relation.getBrand().getId());
        log.info("🛒 카트 아이템 브랜드 ID: {}", cartItem.getProduct().getBrand().getId());

        // when
        Exception exception = assertThrows(InvalidInputException.class, () -> {
            cartItemService.applyCoupon(cartItem.getId(), requestDto, user);
        });

        log.info("❌ 쿠폰 적용 실패 메시지: {}", exception.getMessage());
        assertEquals(ErrorCode.NOT_APPLICABLE_COUPON.getMessage(), exception.getMessage());

        log.info("✅ inValidCoupon 테스트 완료");
    }

    @Test
    void 남이_발급받은_쿠폰을_쓸_수_없다() {
        // 예전에는 사용자 구분 없이 "안 쓴 쿠폰"을 찾아서
        // 다른 사람 쿠폰을 자기 장바구니에 적용할 수 있었다
        User other = userRepository.save(User.builder()
                .email("test77@test.com").password("p").name("남")
                .phoneNum("01077777777").role(Role.CUSTOMER).status(UserStatus.ACTIVE).build());

        ApplyCouponCartRequestDto requestDto = ApplyCouponCartRequestDto.builder()
                .couponId(validCoupon.getId()).build();

        // 본인이 먼저 발급받아 둔다
        cartItemService.applyCoupon(cartItem.getId(), requestDto, user);

        long before = couponUserRepository.countByCouponId(validCoupon.getId());

        CartItem otherCart = cartItemRepository.save(CartItem.builder()
                .product(product).user(other).quantity(1).build());
        cartItemService.applyCoupon(otherCart.getId(), requestDto, other);

        // 남의 발급분을 재사용하지 않고 자기 것을 새로 받아야 한다
        assertEquals(before + 1, couponUserRepository.countByCouponId(validCoupon.getId()));
        assertTrue(couponUserRepository
                .findByCouponIdAndUserUserId(validCoupon.getId(), other.getUserId()).isPresent());
    }
}
