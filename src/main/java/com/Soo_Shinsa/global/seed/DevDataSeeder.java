package com.Soo_Shinsa.global.seed;

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
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 로컬/데모용 시드 데이터.
 * 시드 업주 계정 유무로 판단하므로, 재실행해도 중복 생성하지 않고 기존 데이터도 건드리지 않는다.
 * 운영에서는 app.seed.enabled=false 로 끈다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String SEED_VENDOR_EMAIL = "vendor@sooshinsa.dev";

    /** 카테고리 -> 서브카테고리 */
    private static final Map<String, List<String>> CATEGORIES = Map.of(
            "의류", List.of("상의", "아우터", "바지"),
            "신발", List.of("스니커즈", "부츠"),
            "가방", List.of("백팩", "크로스백")
    );

    /** 브랜드명 -> 서브카테고리, 상품명 목록 */
    private static final List<BrandSeed> BRANDS = List.of(
            new BrandSeed("수신사 베이직", "상의", "군더더기 없는 데일리 기본템",
                    List.of("옥스포드 셔츠", "코튼 반팔 티셔츠", "링클프리 셔츠", "베이직 맨투맨")),
            new BrandSeed("노스브릿지", "아우터", "도심형 아웃도어 아우터",
                    List.of("경량 패딩 점퍼", "고어텍스 3in1 자켓", "플리스 집업", "코치 자켓")),
            new BrandSeed("데님로드", "바지", "핏에 진심인 데님 전문",
                    List.of("와이드 데님 팬츠", "슬림 스트레이트 진", "블랙 세미와이드 진", "코튼 치노 팬츠")),
            new BrandSeed("스텝워크", "스니커즈", "매일 신는 러닝/캐주얼 슈즈",
                    List.of("데일리 러닝화", "레트로 코트 스니커즈", "벌크 어글리 슈즈", "캔버스 로우 스니커즈")),
            new BrandSeed("어반캐리", "백팩", "가볍고 튼튼한 도시형 가방",
                    List.of("15인치 노트북 백팩", "방수 데일리 백팩", "미니 크로스백", "캔버스 토트백"))
    );

    private record BrandSeed(String name, String subCategory, String context, List<String> products) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 시드 업주가 있으면 이미 심어진 것으로 본다 (기존 데이터는 그대로 둔다)
        if (userRepository.findByEmail(SEED_VENDOR_EMAIL).isPresent()) {
            log.info("시드 데이터 생략 - 이미 생성됨");
            return;
        }

        User vendor = userRepository.save(User.builder()
                        .email(SEED_VENDOR_EMAIL)
                        .password(passwordEncoder.encode("Vendor1234!"))
                        .name("데모 업주")
                        .phoneNum("010-0000-0001")
                        .status(UserStatus.ACTIVE)
                        .role(Role.VENDOR)
                        .build());

        // 카테고리 / 서브카테고리
        Map<String, SubCategory> subCategories = new java.util.HashMap<>();
        CATEGORIES.forEach((categoryName, subNames) -> {
            Category category = categoryRepository.save(Category.builder().name(categoryName).build());
            subNames.forEach(subName -> subCategories.put(subName,
                    subCategoryRepository.save(SubCategory.builder().name(subName).category(category).build())));
        });

        // 브랜드 + 상품
        List<Product> products = new ArrayList<>();
        int priceStep = 0;
        for (BrandSeed seed : BRANDS) {
            Brand brand = brandRepository.save(Brand.builder()
                    .registrationNum(String.format("%03d-%02d-%05d", 100 + priceStep, 10 + priceStep, 10000 + priceStep))
                    .name(seed.name())
                    .context(seed.context())
                    .subCategory(subCategories.get(seed.subCategory()))
                    .status(BrandStatus.OPEN)
                    .user(vendor)
                    .build());

            for (String productName : seed.products()) {
                priceStep++;
                products.add(Product.builder()
                        .name(productName)
                        .price(BigDecimal.valueOf(19_000L + priceStep * 7_000L))
                        .productStatus(priceStep % 7 == 0 ? ProductStatus.SOLD_OUT : ProductStatus.AVAILABLE)
                        .brand(brand)
                        .build());
            }
        }
        productRepository.saveAll(products);

        log.info("시드 데이터 생성 완료 - 브랜드 {}건, 상품 {}건", BRANDS.size(), products.size());
    }
}
