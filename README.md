# 無shinsa
## 🛠️ Tools : <img src="https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white"> <img src="https://img.shields.io/badge/spring-6DB33F?style=for-the-badge&logo=github&logoColor=Green"> <img alt="Java" src ="https://img.shields.io/badge/Java-007396.svg?&style=for-the-badge&logo=Java&logoColor=white"/>  <img alt="Java" src ="https://img.shields.io/badge/intellijidea-000000.svg?&style=for-the-badge&logo=intellijidea&logoColor=white"/>, <img src="https://img.shields.io/badge/AmazonAWS-FF0000?style=flat-square&logo=Adobe&logoColor=white">
## 🚩 Period : 2024/01/02 ~ 2024/02/10
## 👨‍💻 ERD <a-href>https://www.erdcloud.com/d/vHWtykYujZaDJdLpc</a-href>
## 👨‍💻 API
<a-href>https://identity.getpostman.com/handover/multifactor?user=41769599&handover_token=3a869f52-d810-4426-b93a-9e4ae97357cd</a-href>
## 👨‍💻 Role 
이해욱 : Order, Toss
  
권진석 : Category, Brand 

김민경 : User, 통계
 
황상익 : Image, Report, Product, ProductOptions, CartItem, Coupon, Review, Cateogry Refactoring, Brand Refactoring, AWS, Order 일부 Refactoring, 각 기능 동적 쿼리 작성 
## 👨‍💻 About Project 
- 쿠팡, 무신사 이커머스를 모티브로 無신사라는 쇼핑몰 이커머스 입니다. 회원가입과 백 오피스 기능이 갖춰져 있고, 회원별 등급에 따라 상품 적립 포인트가 적립이 되어 그에 맞는 할인 및 포인트 사용이 가능하게 할 수 있으며, 관리자와 점주는 판매 현황 및 자신의 매장의 매출 현황을 분석해, 보다 영업을 효율적으로 할 수 있는 기능이 구성되어 있으며, 소비자는 브랜드별 카테고리에 상품들이 구성되어 구매가 가능합니다. 또한 쿠폰 지급을 통해 소비자는 상품을 구매할 때 보다 할인되어 있는 상품을 통해 구매가 가능합니다. 상품 구매 후 후기 작성이 가능 하며, 상품 구매 직전 다른 사람의 후기가 궁금할 경우, 별점에 따라 조회가 가능합니다.
악성 리뷰나, 잘못된 상품이 있을 경우에는 신고기능을 통해, 해당 개시물 혹은 상품을 신고 할 수 있으며, 신고가 접수되면, Admin 측에서 적절한 조치가 이뤄질 수 있도록 했습니다. 
## MVP 1 
- User  
  - UserService
    - CRUD : 회원 가입, 로그인, 회원 조회, 회원 수정, 로그이웃 기능
  - UserController
    - UserService에 회원 가입, 로그인, 회원 조회, 회원 수정, 로그이웃기능 호출
- Brand
  - BrandService
    -  CRUD : 브랜드 생성, 수정, 조회, 브랜드별 점주 조회, 모든 브랜드 조회
  - BrandController
    - BrandService에 브랜드 생성, 수정, 조회, 브랜드별 점주 조회, 모든 브랜드 조회 기능 호출
- Category
  - CategoryService
    - CRUD : 카테고리 생성, 조회 , 수정 기능
  - CateogryController
    - CategoryService에 카테고리 생성, 조회 , 수정 기능 호출
- CartItem
  - CartItemService
    - CRUD : 장바구니 생성, 장바구니 전체 조회 (날짜 선택 조회), 장바구니 수정, 장바구니에서 구매 전 쿠폰이 있다면 쿠폰 적용
  - CartItemController
    - 장바구니 생성, 장바구니 전체 조회 (날짜 선택 조회), 장바구니 수정, 장바구니에서 구매 전 쿠폰이 있다면 쿠폰 적용 기능 호출
- Product
  - ProductService
    - CRUD : 상품 생성, 수정, 단일 상품 조회, 이름 내림차순 상품 조회, 상품 삭제
  - ProductController
    - 상품 생성, 수정, 단일 상품 조회, 이름 내림차순 상품 조회, 상품 삭제 기능 호출 
- Review
  - ReviewService
    - CRUD : 리뷰 생성, 리뷰 조회, 리뷰 수정, 리뷰 별점에 따라 조회 기능, 리뷰 삭제
  - ReviewController
    - 리뷰 생성, 리뷰 조회, 리뷰 수정, 리뷰 별점에 따라 조회 기능, 리뷰 삭제 호출
- Report
  - ReportService
    - CRUD : 신고 생성, 신고 상태 변경, 신고 조회, 모든 신고 조회, 신고 삭제
  - ReviewController
    - 신고 생성, 신고 상태 변경, 신고 조회, 모든 신고 조회, 신고 삭제 호출
- Coupon
  - CouponService
    - 쿠폰 생성, 쿠폰 생성시 분산락을 활용해, 동시성 제어를 설정했습니다.
      비관적 락, 낙관적 락을 사용하면 동시성에 문제는 해결되지만 성능 저하 가능성, 특히 트랜잭션이 많이질수록 DB에 락이 증가하여 성능이 떨어지거나 데드락 발생 가능성 증가. 이를 해력하기 위해 Redisson과 같은 In-Memory 데이터 저장소를 활용한 분산 락을 적용하면, 빠른 속도로 동시성을 제어하면서 성능 저하 없이 쿠폰 발급을 처리가 가능 하고  In-Memory 데이터 저장소를 활용한 분산 락을 적용하면, 빠른 속도로 동시성을 제어하면서 성능 저하 없이 쿠폰 발급을 처리가 가능합니다. 
  - CouponContoller
    - 쿠폰을 생성하는데 호출
- Static
  - StaticService
    - 현재 매출 상태를 확인, 통계 합계
  - StaticController
    - StaticService 호출

## 🧨 TroubleShooting 
1. QueryDSL 문제 해결
- 문제점
동적 쿼리를 Service에서 작성했으나, Repository의 책임이 Service로 전가되는 문제 발생.
Entity로 직접 데이터를 조회했더니 불필요한 컬럼까지 함께 조회되는 문제 발생.
DTO 필드 순서를 맞추지 않으면 다음과 같은 오류 발생:
```com.querydsl.core.types.ExpressionException: No constructor found for class com.Soo_Shinsa.order.dto.OrderItemResponseDto```

- 해결방법

✅ CustomRepository를 생성하여 동적 쿼리를 Service가 아닌 Repository에서 수행.  
✅ DTO 반환 시 Projections.constructor(dto.class) 를 사용하여 필요한 컬럼만 조회하도록 변경.   
✅ DTO 필드 순서를 맞추고, 필요한 필드만 포함하여 오류 방지.

2. 쿠폰 관련 문제 해결
문제 1 - Deadlock 발생
- 현상
4000개 요청이 동시에 쿠폰 발급을 시도, 그러나 발급 가능 개수는 10개로 제한.
여러 스레드가 동시에 같은 쿠폰을 가져와 발급하려다 경합 상태 발생 → issuedCount 값이 꼬이는 문제 발생.
- 해결방법

✅ 비관적 락(Pessimistic Lock) 적용하여 트랜잭션 종료 전까지 데이터 변경 차단.

문제 2 - 쿠폰 재고 문제
- 현상
쿠폰이 존재하지만 사용자가 접근할 경우 NOT_FOUND_COUPON 예외 발생.
- 원인: 기존 코드에서 특정 사용자의 CouponUser만 조회하여, 다른 사용자가 접근할 경우 새로운 CouponUser를 찾지 못함.
- 해결방법

✅ 쿠폰 재고(maxCount)가 남아 있다면 새로운 CouponUser를 생성하도록 로직 변경.
```
@Transactional
public ApplyCouponCartResponseDto applyCoupon(Long cartId, ApplyCouponCartRequestDto requestDto, User user) {
    CartItem cartItem = cartItemRepository.findByIdOrElseThrow(cartId);

    String lockKey = "coupon-lock:" + requestDto.getCouponId();
    RLock lock = redissonClient.getLock(lockKey);

    try {
        if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("현재 쿠폰 사용 요청이 많아 잠시 후 다시 시도해주세요.");
        }

        Optional<CouponUser> optionalCouponUser = couponUserRepository.findUnusedCouponByCouponId(requestDto.getCouponId());

        Coupon coupon;
        CouponUser couponUser;

        if (optionalCouponUser.isPresent()) {
            couponUser = optionalCouponUser.get();
            coupon = couponUser.getCoupon();
        } else {
            coupon = couponRepository.findById(requestDto.getCouponId())
                    .orElseThrow(() -> new InvalidInputException(ErrorCode.NOT_FOUND_COUPON));

            if (coupon.getMaxCount() <= 0) {
                throw new InvalidInputException(ErrorCode.COUPON_OUT_OF_STOCK);
            }

            couponUser = CouponUser.builder()
                    .coupon(coupon)
                    .user(user)
                    .isUsed(false)
                    .usedAt(null)
                    .build();

            couponUserRepository.save(couponUser);
        }

        if (coupon.isExpired()) {
            throw new InvalidInputException(ErrorCode.EXPIRED_COUPON);
        }

        DiscountCouponCalculator discountCouponCalculator = new PercentageDiscountCalculator();
        BigDecimal discountPrice = discountCouponCalculator.calculateDiscountedPrice(cartItem.getProduct().getPrice(), coupon.getDiscountRate());

        cartItem.applyCoupon(coupon, discountPrice);
        cartItemRepository.save(cartItem);

        couponUser.markAsUsed();
        coupon.decreaseMaxCount(1);
        couponUserRepository.save(couponUser);

        List<ProductOption> productOptions = productOptionRepository.findProductOptionByProductId(cartItem.getProduct().getId());
        return ApplyCouponCartResponseDto.toDto(cartItem, productOptions);

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("쿠폰 적용 중 오류가 발생했습니다.", e);
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```
3. Order 결제 및 쿠폰 할인 적용 문제 해결
4. 문제 1 - 결제 API 연동
- 현상
secretKey를 Base64.getEncoder()로 인코딩한 후, restTemplate을 통해 orderId 및 amount를 TossPayments API에 전달해야 했음.
외부 결제 API 호출 경험이 부족하여 처리 흐름을 이해하는 데 어려움이 있었음.

- 해결방법

✅ TossPayments 개발자 센터 문서를 참고하여 결제 API 흐름을 학습.  
✅ API 연동 후, 결제가 완료되면 paymentKey를 DB에 저장하도록 구현.  
✅ 결제 취소 기능을 추가하여 paymentKey를 이용해 결제 취소 가능하도록 개선.

```
@Transactional
public void approvePayment(String paymentKey, String orderId, Long amount, Model model) throws JsonProcessingException {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes()));
    headers.setContentType(MediaType.APPLICATION_JSON);

    Payment findPayment = paymentRepository.findByOrderId(orderId);
    findPayment.update(TossPayStatus.DONE, paymentKey);
    paymentRepository.save(findPayment);

    PayloadRequestDto payload = new PayloadRequestDto(orderId, String.valueOf(amount));
    HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
    ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity(
            "https://api.tosspayments.com/v1/payments/" + paymentKey, request, JsonNode.class);

    Orders findOrder = ordersRepository.findByOrderId(orderId);
    findOrder.updateStatus(PAYMENTCOMPLETED);
    ordersRepository.save(findOrder);
}

```
✅ 이점:

결제 및 취소 프로세스를 안정적으로 구축하여 사용자 경험 향상.
외부 결제 API 연동 경험을 쌓아 향후 확장 가능성 증가.

4. 상품 할인 적용 문제 해결
- 문제 : 주문 시 쿠폰 적용 후 할인 금액이 totalAmount에 반영되지 않는 현상
- 현상
할인 금액이 totalAmount에 반영되지 않아, 최종 결제 금액이 변경되지 않는 문제 발생.
- 해결방법

✅ 할인 금액이 적용되지 않는 기존 로직을 수정.  
✅ OrderItems에 discountPrice 필드를 추가하여 할인된 가격을 반영하도록 개선.

```
// 총 결제 금액 계산
public void calculateTotalPrice() {
    this.totalPrice = orderItems.stream()
            .map(item -> {
                BigDecimal effectivePrice = (item.getDiscountPrice() != null && item.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0)
                        ? item.getDiscountPrice()
                        : item.getProduct().getPrice();
                return effectivePrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}

```
✅ 할인 금액이 정상적으로 반영되어 최종 결제 금액이 정확하게 계산됨.  
✅ 쿠폰 적용 시에도 할인된 가격을 반영하여 정합성 유지.

5. 상품 조회 시 파라미터 오류 해결
문제점
- 현상
  상품 조회 시 다음과 같은 오류 발생
  ```
  org.springframework.dao.InvalidDataAccessApiUsageException: At least 2 parameter(s) provided but only 1       parameter(s) present in query java.lang.IllegalArgumentException: At least 2 parameter(s) provided but only 1 parameter(s) present in query
  ```
- 원인
JPQL을 사용하는 과정에서 매개변수 개수가 일치하지 않아 발생한 문제.

- 해결방법  
✅ 기존 QueryDSL 사용 방식 대신 JPQL로 변경하여 오류 해결.  
✅ 필요한 매개변수를 명확하게 지정하여 호출하도록 수정.

6. 이미지 처리 문제 해결
문제점
- 현상
S3를 사용하여 이미지를 업로드하는 과정에서 S3를 직접 접근하는 방식이 유지보수에 어려움을 초래.
이미지 업로드 시 파일명이 없거나 확장자가 없을 경우 OutOfIndex 오류 발생.
- 해결방법  
✅ S3 접근 로직을 ImageService로 분리하여 유지보수 용이하도록 개선.  
✅ 파일명을 FileUtils 유틸 클래스를 통해 안전하게 처리.  
✅ 확장자가 없는 경우 예외 처리 추가.

파일 확장자 및 MIME 타입 검증 코드 추가
```
public enum FileType {
    JPG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    GIF("gif", "image/gif"),
    JPEG("jpeg", "image/jpeg");

    private final String extension;
    private final String mimeType;

    FileType(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    /**
     * 확장자와 MIME 타입의 유효성을 검증
     */
    public static boolean isValid(String extension, String mimeType) {
        return Arrays.stream(values())
                .anyMatch(fileType -> fileType.extension.equals(extension) && fileType.mimeType.equals(mimeType));
    }
}

```

파일명 안전하게 처리하는 FileUtils 클래스 추가
```
public class FileUtils {

    /**
     * 파일 이름에서 확장자를 추출
     *
     * @param originName 원본 파일명
     * @return 확장자 (소문자)
     * @throws IllegalArgumentException 확장자가 없을 경우 예외 발생
     */
    public static String extractFileExtension(String originName) {
        int dotIndex = originName.lastIndexOf(".");
        if (dotIndex == -1 || dotIndex == originName.length() - 1) {
            throw new InvalidInputException(ErrorCode.NO_EXTENSION);
        }
        return originName.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * 파일 이름에서 확장자를 제외한 이름만 추출
     *
     * @param originName 원본 파일명
     * @return 확장자를 제외한 파일명
     */
    public static String extractFileName(String originName) {
        int dotIndex = originName.lastIndexOf(".");
        if (dotIndex == -1) {
            return originName; // 확장자가 없는 경우 전체 파일명 반환
        }
        return originName.substring(0, dotIndex); // 확장자 이전의 파일명 반환
    }
}

```
Image 엔티티 생성자에서 파일명 안전하게 처리
```
public Image(String originName, TargetType targetType) {
    // 안전한 확장자 추출
    this.originName = FileUtils.extractFileName(Objects.requireNonNull(originName, "파일명은 필수입니다."));
    this.extension = FileUtils.extractFileExtension(originName);

    // 저장 파일명은 UUID로 생성
    this.name = UUID.randomUUID().toString();
    this.path = determinePath(targetType);
}
```
✅ 확장자가 없는 경우 예외 발생하여 안전성 향상.  
✅ UUID를 사용하여 저장 파일명을 생성함으로써 중복 문제 방지.  
✅ S3 업로드 시 ImageService를 통해 로직을 분리하여 유지보수 용이.

7. 카테고리 계층 구조 문제 해결
문제점
- 현상
자식 카테고리 생성 시 부모 ID가 null로 저장되는 문제 발생.
조회 시 부모-자식 관계를 명확하게 가져오지 못함.
- 해결방법  
✅ 재귀 함수 활용하여 계층 구조 유지  
✅ 자식 카테고리 조회 시 부모 정보까지 포함하도록 개선

```
public class CategoryResponseDto {
    private Long id;
    private String name;
    private CategoryResponseDto parent;

    public CategoryResponseDto(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        // 부모 카테고리가 존재하면 재귀적으로 생성
        this.parent = (category.getParent() != null) ? new CategoryResponseDto(category.getParent()) : null;
    }

    public static CategoryResponseDto toDto(Category category) {
        return new CategoryResponseDto(category);
    }
}

```
✅ 부모 카테고리를 포함한 DTO 변환을 적용하여 계층 구조 유지  
✅ 재귀 호출을 통해 부모-자식 관계를 명확하게 가져올 수 있도록 개선  
✅ 조회 시 DB 불필요한 추가 호출을 최소화하여 성능 최적화

8. Docker(도커) 관련 문제 해결
문제점
- 현상
기존 AWS 환경에서 EC2 + Nginx + Spring Boot를 직접 배포하는 방식 사용 중.
Docker를 활용하여 컨테이너 기반 배포를 하려 했으나 Redis 포트 문제 발생
```
Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.
2025-02-07T22:49:41.810+09:00 ERROR 35517 --- [Soo-Shinsa] [           main] o.s.boot.SpringApplication               : Application run failed
org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'cartItemController'
org.springframework.beans.factory.UnsatisfiedDependencyException: Failed to instantiate [org.redisson.api.RedissonClient]

```

- 원인
Redis 컨테이너가 Docker Network에서 제대로 연결되지 않거나, 포트 충돌 발생.
- 추후 해결방법  
✅ docker-compose.yml을 사용하여 Redis 포함한 컨테이너 환경 구성.  
✅ Redis 컨테이너가 실행되지 않는 경우 depends_on 옵션을 사용하여 의존성 해결.  
✅ 환경 변수 설정을 통해 포트 충돌을 방지하고, RedissonClient 설정을 수정.

## 🫠 MVP2에서 해야 할 것들 
1. Token 형성시 RefreshToken과 BlakcList에 대한 해당 기능 들이 없다. 로그아웃 시 해당 토큰을 삭제하는 것이 아닌 따로 관리 해줄 필요가 있다. 
그래서 이를 추후에 추가해 관리 할 예정
2. 쿠폰을 보면 현재는 브랜드만 적용 할 수 있도록 되어 있다. 상품에도 적용 할 수 있도록 상품과 쿠폰을 정규화를 진행하고, 적용할 예정이다.
3. 카테고리에 대한 depth가 너무 얕다. 그래서 depth를 추가적으로 적용할 예정
4. 브랜드에 대한 카테고리 추가 생성필요
5. docker 생성과 git actions을 통한 자동화 배포 진행 예정

## MVP 2 
## 🧨 Trouble Shooting 
### 1️⃣ 연관 관계 변경
변경된 관계:
Category -> SubCategory -> Brand -> Product
(기존: Brand -> Category -> SubCategory)

🔍 문제점
기존에는 Brand 안에 Category가 있고, 그 안에 SubCategory가 들어가는 구조.
그러나 논리적으로 Category(대분류) -> SubCategory(소분류) -> Brand(브랜드) 순으로 배치하는 것이 더 적절함.

🛠 해결 방법
Brand가 아니라 Category에서부터 SubCategory를 연결.
SubCategory 안에서 Brand가 연결되도록 수정.


### 2️⃣ 쿠폰 AOP 적용 (분산 락 & 트랜잭션 관리 문제 해결)
🔍 문제점
기존 코드에서는 @Transactional 내부에서 분산 락을 처리하고 있었음.
하지만 트랜잭션이 실패할 경우 락이 해제되지 않을 가능성이 있음.
원인: 트랜잭션이 롤백되면 락도 반납되지 않을 수 있음 → DeadLock 발생 가능성이 증가.

🛠 해결 방법  
✅ AOP를 활용하여 락을 선 실행하고 이후 트랜잭션을 시작하도록 변경  
✅ 트랜잭션 없이 락만 사용하는 읽기 작업도 가능하도록 설계

```
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponLockAspect {
    private final RedissonClient redissonClient;

    @Around("@annotation(couponLock)")
    public Object lock(ProceedingJoinPoint joinPoint, CouponLock couponLock) throws Throwable {
        String lockKey = couponLock.key();
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = false;
        try {
            acquired = lock.tryLock(couponLock.waitTime(), couponLock.leaseTime(), TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException("현재 쿠폰 발급 요청이 많아 잠시 후 다시 시도해주세요.");
            }
            return joinPoint.proceed();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```
```
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CouponLock {
    String key(); // 락 키
    long waitTime() default 10; // 대기 시간
    long leaseTime() default 10; // 락 유지 시간
 }
```

### 3️⃣ BlackList & Refresh Token 적용
(Security Stateless한 특성 활용)

🔍 문제점
기존에는 Security를 적용했지만 토큰만 사용하고 있어서 stateless한 장점을 살리지 못함.
Access Token이 만료될 때마다 사용자가 로그인해야 하는 불편함 발생.
Access Token이 탈취되었을 경우 보안 리스크 존재.

🛠 해결 방법  
✅ BlackList 적용 (Redis에 저장하여 무효화 처리)  
✅ Refresh Token 활용하여 Access Token 재발급 시스템 도입

###  로그인/회원가입 URL 필터 처리 오류 해결
🔍 문제점
```Invalid JWT token: 토큰이 비어 있습니다.```
Spring Security 필터 순서 문제
UsernamePasswordAuthenticationFilter보다 뒤에서 실행되어 JWT 검증이 먼저 발생.
JwtAuthFilter에서 로그인/회원가입 URL이 필터 제외 대상에서 빠짐.
로그인 & 회원가입 요청에서도 JWT 검증을 시도 → 토큰이 없으므로 Invalid JWT token 오류 발생.

🛠 해결 방법  
✅ Security 필터 순서를 UsernamePasswordAuthenticationFilter보다 앞으로 조정!  
✅ 로그인 & 회원가입 API는 필터 예외 처리!

```
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
    
    String requestURI = request.getRequestURI();
    
    // 로그인 & 회원가입 요청은 필터링 제외
    if (requestURI.equals("/users/login") || requestURI.equals("/users/signin")) {
        filterChain.doFilter(request, response);
        return;
    }

    // 기존 JWT 인증 처리 로직 수행
    this.authenticate(request);
    filterChain.doFilter(request, response);
}

```

### 4️⃣ SecurityContextHolder.getContext().getAuthentication()이 null이거나 올바른 객체가 아님
🔍 문제 발생 상황  
로그아웃을 시도했을 때, SecurityContextHolder.getContext().getAuthentication()이 null이거나 UserDetailsImp 객체가 아닌 것으로 인식됨.  
결과적으로 "인증이 필요합니다. 로그인 후 이용해주세요." 메시지가 출력됨.

🛠 해결 방법  
JWT 필터에서 로그아웃 요청("/users/logout")을 그냥 통과시키고 있었음.  
JwtAuthFilter에서 로그아웃 요청을 인증 없이 통과시키고 있었기 때문에, SecurityContextHolder에 인증 객체가 저장되지 않음.

### 5️⃣ SecurityConfig에서 로그아웃 URL이 인증 없이 접근 가능하도록 되어 있었음
🔍 문제 발생 상황  
UrlConst라는 클래스 내부에 WhiteList라는 List 값 안에 users/logout 이라는 기능이 없었음. 

🛠️ 해결 방법
```public static final String[] WHITE_LIST = {"/", "/users/signin", "/users/refresh", "/users/logout", "/users/login", "/api/**", "/test","/stylesheets/**","/success"};```
추가 

### 6️⃣ 로그인 후 SecurityContext에 인증 정보가 저장되지 않음

🔍 문제 발생 상황  
로그인 후 SecurityContextHolder.getContext().setAuthentication(auth);이 실행되지 않거나, Redis에서 저장된 토큰이 없어서 인증이 초기화됨.

🛠 해결 방법
1. 로그인 시 SecurityContextHolder에 인증 정보 저장 확인
```
Authentication auth = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
                dto.getEmail(),
                dto.getPassword()
        )
);

// ✅ SecurityContext에 인증 객체 저장
SecurityContextHolder.getContext().setAuthentication(auth);
log.info("로그인 성공: SecurityContextHolder에 인증 객체 저장됨. 사용자: {}", dto.getEmail());
```
2. Redis를 사용 하고 있기 때문에
```
String storedAccessToken = jwtAccessTokenService.getAccessToken(token);
if (storedAccessToken == null) {
    log.warn("Redis에서 AccessToken을 찾을 수 없음! username: {}", username);
}

```

### 👨‍💻 Token을 Redis로 관리한 이유 
1. 성능 향상 (Fast Authentication)
    -  DB를 이용한 토큰 검증은 성능이 느림
     -  JWT를 사용할 때, 일반적으로 사용자 정보를 검증하기 위해 DB에서 유저를 조회해야 함.
     -  이는 매 요청마다 DB 조회가 발생하여 성능 저하를 유발.
       
🛠 Redis를 사용한 해결 방법
- JWT와 사용자 정보를 Redis에 캐싱하여, DB 조회 없이 빠르게 인증 가능.

📌 효과  
✅ 매 요청마다 DB를 조회할 필요 없이 빠른 인증 처리  
✅ DB 부하를 줄이고 서버 성능을 최적화

2. 자동 로그아웃 처리 (Token Expiry Management)
JWT 자체적으로 만료된 토큰을 자동으로 정리하지 않음.
만약 로그아웃한 사용자의 Refresh Token을 명시적으로 삭제하지 않으면, 보안 리스크가 발생할 수 있음.

🛠 Redis를 사용한 해결 방법  
Redis의 EXPIRE 기능을 이용해, 자동으로 만료된 토큰을 제거.

📌 효과  
✅ 만료된 토큰을 자동으로 삭제하여 보안 강화  
✅ 만료된 Refresh Token이 남아 있지 않도록 관리 가능

### 카카오톡 OAuth2 요청시 state 제거  
🔍 문제 상황  
카카오 OAuth2를 이용하여 액세스 토큰을 요청할 때, state 값이 포함된 code 값을 전송하면 API 요청이 실패하는 문제가 발생함.  
Swagger UI에서 code 값을 직접 요청하여 토큰을 가져오려 하면 400 Bad Request 오류 발생  
같은 code 값을 브라우저에서 복사하여 요청하면 정상적으로 액세스 토큰을 받을 수 있음  
Swagger UI에서 받아온 code 값이 state 값을 포함하고 있음  
state 값을 제거하면 정상적으로 토큰 요청이 수행됨

🛠️ 해결 방법  
Spring Boot에서 WebClient를 이용하여 카카오 API에 요청하는 로직을 수정하여 code 값에서 state 값을 자동으로 제거하도록 설정함.  

### Chatting 기능 구현 
<a-href>https://velog.io/@ik0605/%EC%B1%84%ED%8C%85-%EA%B5%AC%ED%98%84-%ED%94%84%EB%A1%9C%EA%B7%B8%EB%9E%A8</a-href>
1️⃣ 브라우저에서 WebSocket 차단 (Content Security Policy 오류)  
```
Refused to connect to 'ws://localhost:8080/ws/chat/123' because it violates the following Content Security Policy directive: "connect-src 'self'".
```  
- 브라우저에서 WebSocket 요청 차단됨.  
🛠️ 해결방법 
```
.headers(headers -> headers
        .contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; " +
                "connect-src 'self' ws://localhost:8080 ws://127.0.0.1:8080; " +
                "script-src 'self' 'unsafe-inline'; " +
                "style-src 'self' 'unsafe-inline';"
        ))
)

```  
2️⃣ WebSocket 연결 직후 바로 끊기는 문제 (EOFException 발생)  
🚨 문제  
```
java.io.EOFException: null
at org.apache.tomcat.websocket.server.WsFrameServer.onDataAvailable(WsFrameServer.java:74)
```   
- WebSocket이 연결된 직후 EOFException 발생 -> 종료
- WebSocket은 연결된 로그는 찍히지만 곧바로 WebSocket 연결 종료  
🛠 해결 방법  
```
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    log.info("✅ WebSocket 연결됨: 세션 ID: {}", session.getId());
    try {
        session.sendMessage(new TextMessage("ping"));
    } catch (IOException e) {
        log.error("🚨 WebSocket 초기 메시지 전송 실패", e);
    }
}

```





