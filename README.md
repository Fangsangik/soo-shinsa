# 無Shinsa

## 📌 프로젝트 개요

**無Shinsa**는 쿠팡 및 무신사와 같은 이커머스를 모티브로 한 온라인 쇼핑몰 프로젝트입니다. 회원가입 및 백오피스 기능을 포함하여, 회원 등급에 따라 포인트 적립, 할인, 포인트 사용이 가능하며, 관리자와 점주는 매출 및 판매 현황을 분석하여 운영 효율성을 높일 수 있습니다. 소비자는 브랜드별 카테고리에서 상품을 탐색 및 구매할 수 있으며, 쿠폰 시스템을 통해 추가적인 할인 혜택을 누릴 수 있습니다.

## 🛠️ 기술 스택

- **Backend**: Java, Spring Boot, JPA, QueryDSL,  Sokcet.I/O
- **Database**: MySQL, Redis
- **Infra**: AWS (S3, EC2, RDS)
- **API Management**: Postman, Swagger
- **Payment**: Toss Payments
- Authentication :  SpringSecurity, Kakao
- CORS : Simple global configuration so the React frontend can call the API

## 🌐 Frontend

The project ships with a small React based UI under `frontend`.  Open
`frontend/index.html` directly in your browser and you will be able to log
in with your user account and navigate through several screens.  Navigation is
handled via React Router so no build step is required.  From the UI you can
look up products, browse categories and brands, view your cart and your past
orders.

## 🌐 Frontend

The project ships with a small React based UI under `frontend`.  Open
`frontend/index.html` directly in your browser and you will be able to log
in with your user account and navigate through several screens.  Navigation is
handled via React Router so no build step is required.  From the UI you can
look up products, browse categories and brands, view your cart and your past
orders.

---

## 🚀 프로젝트 기간

- **MVP 1**: 2025/01/02 ~ 2025/02/10
- **MVP 2**: 2025/02/17 ~ 2025/02/25

## 🎯 주요 기능

### 1. 사용자(User)

- 회원가입, 로그인, 회원 조회, 회원 수정, 로그아웃 기능 제공
- JWT 기반 인증 및 Refresh Token 관리 추가 예정
- 카카오톡을 통해 로그인 가능

### 2. 브랜드(Brand)

- 브랜드 생성, 수정, 조회, 브랜드별 점주 조회 기능 제공

### 3. 카테고리(Category)

- 카테고리 생성, 조회, 수정 기능

### 4. 서브 카테고리 (SubCategory)

- 카테고리와 서브 카테고리 정규화

### 5. 장바구니(CartItem)

- 장바구니 생성, 날짜별 조회, 수정, 구매 전 쿠폰 적용 기능

### 6. 상품(Product)

- 상품 생성, 수정, 조회 (단일 상품 조회, 이름 내림차순 정렬), 삭제 기능

### 7. 리뷰(Review)

- 리뷰 생성, 조회, 수정, 별점별 조회, 삭제 기능

### 8. 신고(Report)

- 신고 생성, 상태 변경, 조회, 삭제 기능
- 신고 접수 후 관리자(Admin) 조치 가능

### 9. 쿠폰(Coupon)

- 쿠폰 생성 시 Redisson 기반 **분산 락** 적용하여 동시성 제어
- 비관적 락 & 낙관적 락 대비 성능 저하 문제 해결
- 쿠폰 재고 관리 및 만료일 검증 로직 추가
- 쿠폰을 브랜드뿐만 아니라 개별 상품에도 적용 가능하도록 정규화 예정

### 10. 결제(Payment)

- Toss Payments API 연동하여 결제 및 취소 기능 구현
- Base64 인코딩 처리 및 PaymentKey 저장 방식 적용
- 결제 완료 시 주문 상태 업데이트

### 11. 통계(Static)

- 매출 통계 및 주문 관련 데이터 제공

### **12. 채팅 (Chatting)**

- 관리자에게 채팅으로 문의 가능

---

## 🛠️ 문제 해결 및 트러블슈팅

# **MVP1**

## 🚀 QueryDSL 관련 문제 해결

### **🔍 문제점:**

- Service 계층에서 동적 쿼리 작성으로 인해 책임이 Repository로 이동하지 못하는 문제
- Entity 직접 조회 시 불필요한 컬럼까지 포함되는 문제 발생
- DTO 필드 순서 불일치로 인해 예외 발생

### **🛠️ 해결 방법:**

- **CustomRepository** 생성하여 동적 쿼리를 수행
- DTO 반환 시 **Projections.constructor(dto.class)** 사용하여 필요한 컬럼만 조회
- DTO 필드 순서 맞추고 불필요한 필드 제거

## 🚀 쿠폰 동시성 문제 해결

### **🔍 문제점:**

- 쿠폰 동시 발급 요청으로 인해 issuedCount 값 꼬임 문제 발생
- 쿠폰 재고가 남아 있어도 특정 사용자가 접근 시 NOT_FOUND_COUPON 예외 발생

### **🛠️  해결 방법:**

- **Redisson 기반 분산 락 적용** (tryLock을 사용하여 동시성 제어)
- 쿠폰 재고(maxCount) 체크 후 새로운 CouponUser 생성하도록 변경
- 쿠폰 사용 시 만료 여부 확인 추가

## 🚀 결제 API 연동 및 할인 적용 문제 해결

### **🔍 문제점:**

- TossPayments API 연동 경험 부족으로 처리 흐름 이해 어려움
- 쿠폰 적용 후 총 결제 금액 반영되지 않는 문제 발생

### **🛠️  해결 방법:**

- TossPayments 공식 문서 참고하여 API 호출 및 결제 흐름 이해
- 결제 완료 후 **paymentKey 저장** 및 주문 상태 업데이트
- **할인 금액 반영 로직 수정** (OrderItems에 discountPrice 필드 추가하여 할인 금액 반영)

## 🚀 이미지 업로드 문제 해결 (AWS S3)

### **🔍 문제점:**

- S3 직접 접근 방식으로 인해 유지보수 어려움
- 확장자 없는 파일 업로드 시 OutOfIndex 오류 발생

### **🛠️  해결 방법:**

- **ImageService로 업로드 로직 분리**하여 유지보수 용이
- 파일명 처리 유틸 클래스(FileUtils) 추가하여 확장자 유효성 검사
- **UUID 기반 파일명 생성**으로 중복 문제 방지

## 🚀 Docker(도커) 관련 문제 해결 문제점

### **🔍 문제점 :**

- 현상 기존 AWS 환경에서 EC2 + Nginx + Spring Boot를 직접 배포하는 방식 사용 중. Docker를 활용하여 컨테이너 기반 배포를 하려 했으나 Redis 포트 문제 발생

### **🛠️  해결방법**

docker-compose.yml을 사용하여 Redis 포함한 컨테이너 환경 구성. 
Redis 컨테이너가 실행되지 않는 경우 depends_on 옵션을 사용하여 의존성 해결.
환경 변수 설정을 통해 포트 충돌을 방지하고, RedissonClient 설정을 수정.

---

# **MVP2**

## 🚀 연관 관계 변경

### 🔍 기존 구조

- **Brand -> Category -> SubCategory**

### 🛠 개선 후 구조

- **Category -> SubCategory -> Brand -> Product**

**문제점:** 

- 기존 구조에서는 브랜드 내에 카테고리가 포함되어 논리적으로 맞지 않음.

**해결 방법:** 

- Category에서 SubCategory를 연결하고, SubCategory에서 Brand가 연결되도록 수정.
- 카테고리와 서브 카테고리를 정규화 하여 중복 데이터를 감소, 참조 무결성 유지, 검색 및 조회 기능 성능 향상

---

## 🚀 쿠폰 AOP 적용 (분산 락 & 트랜잭션 문제 해결)

### 🔍 문제점

- 기존에는 @Transactional 내부에서 분산 락을 처리하여 트랜잭션 실패 시 락이 해제되지 않을 가능성이 있음.
- 트랜잭션이 롤백되면 락이 반납되지 않아 DeadLock 발생 가능성이 존재.

### 🛠 해결 방법

✅ AOP를 활용하여 락을 선 실행 후 트랜잭션을 시작하도록 변경
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

---

## 🚀 BlackList & Refresh Token 적용

### 🔍 문제점

- 기존에는 Access Token이 만료될 경우, 사용자가 로그인해야 하는 불편함 존재.
- Access Token이 탈취될 경우 보안 리스크 존재.

### 🛠 해결 방법

✅ BlackList 적용 (Redis에 저장하여 무효화 처리)
✅ Refresh Token 활용하여 Access Token 재발급 시스템 도입

```
SecurityContextHolder.getContext().setAuthentication(auth);
log.info("로그인 성공: SecurityContextHolder에 인증 객체 저장됨. 사용자: {}", dto.getEmail());
```

---

## 🚀 로그인/회원가입 URL 필터 처리 오류 해결

### 🔍 문제점

- Security 필터 순서 오류로 인해 로그인 & 회원가입 요청에서도 JWT 검증이 시도됨.
- Invalid JWT token 오류 발생.

### 🛠 해결 방법

✅ Security 필터 순서를 조정하여 로그인 & 회원가입 API 예외 처리 추가.

```
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
    String requestURI = request.getRequestURI();
    if (requestURI.equals("/users/login") || requestURI.equals("/users/signin")) {
        filterChain.doFilter(request, response);
        return;
    }
    this.authenticate(request);
    filterChain.doFilter(request, response);
}
```

---

## 🚀 SecurityContextHolder 문제 해결

### 🔍 문제점

- 로그아웃 시 SecurityContextHolder에서 인증 정보가 사라지는 문제.

### 🛠 해결 방법

✅ 로그아웃 요청이 SecurityContextHolder를 거치도록 수정
✅ WhiteList에 "/users/logout" 추가

```
public static final String[] WHITE_LIST = {"/users/login", "/users/signin", "/users/logout", "/users/refresh"};
```

---

## 🚀 로그인 후 SecurityContext에 인증 정보가 저장되지 않는 문제 해결

### 🔍 문제점

- 로그인 후 Redis에서 저장된 토큰이 없어 인증 초기화됨.

### 🛠 해결 방법

✅ SecurityContextHolder에 인증 객체 저장 로직 추가
✅ Redis에서 Access Token을 가져와 확인

```
String storedAccessToken = jwtAccessTokenService.getAccessToken(token);
if (storedAccessToken == null) {
    log.warn("Redis에서 AccessToken을 찾을 수 없음! username: {}", username);
}
```

---

## 🚀 카카오톡 OAuth2 요청 시 state 제거

### 🔍 문제점

- Swagger UI에서 받은 code 값이 state 값을 포함하여 API 요청 실패.

### 🛠 해결 방법

✅ WebClient 요청에서 state 값을 제거하도록 수정.

---

## 🚀 WebSocket (채팅) 기능 개선

### 🔍 문제점

1️⃣ 브라우저에서 WebSocket 요청 차단 (CSP 오류 발생)
2️⃣ WebSocket 연결 직후 바로 끊기는 문제 발생 (EOFException)

### 🛠 해결 방법

✅ Content Security Policy 수정
✅ WebSocket 연결 후 핑 메시지 전송 추가

```jsx
@Override
public void afterConnectionEstablished(WebSocketSession session) {
[log.info](http://log.info/)("✅ WebSocket 연결됨: 세션 ID: {}", session.getId());
	try {
session.sendMessage(new TextMessage("ping"));
	} catch (IOException e) {
log.error("🚨 WebSocket 초기 메시지 전송 실패", e);
 }
}
```

---

## **🚀 JWT 인증 시 Redis에서 AccessToken을 찾을 수 없음**

### **🔍 문제점**

- SecurityContext에 인증 정보는 저장되었으나, Redis에서 AccessToken을 찾을 수 없음.
- Redis CLI를 통해 확인했을 때, AccessToken이 저장되어 있음에도 불구하고 찾지 못하는 현상 발생.

### **🛠️ 해결 방법:**

- **저장할 때와 조회할 때 동일한 키 형식 사용 (`access_Token + email`)**
    
    ---
    

## **🚀 Hibernate에서 동일한 사용자 조회 쿼리가 두 번 실행되는 문제**

### **🔍 문제점**

- **로그인시 쿼리가 4번 실행**

### **🤯 원인**

- **KakaoUser와 UserGrade에 의한 LazyLoading N + 1 문제**
- `login()`에서 `findByEmailOrElseThrow()`로 사용자 조회 후, Security의 `UserDetailsService.loadUserByUsername()`이 호출되면서 **중복 조회 발생.**

### **🛠️ 해결 방법:**

- **N + 1 문제의 경우 ⇒ Join Fetch로 해결**
- `authenticationManager.authenticate()`**를 사용하지 않고, 직접 SecurityContext에 인증 정보 설정**

## **🚀 쿠폰 동시성 처리 - CartItemServiceImpl에서 OrderServiceImpl로 이동**
### **🔍 문제점**  
장바구니(CartItemServiceImpl)에서 쿠폰을 적용할 때 동시성 문제가 발생  
- 기존 로직에서는 쿠폰을 장바구니에서 적용 => maxCount 감소  
- 장바구니에 쿠폰을 적용한 후 주문을 하지 않았음에도 재고가 소진

### **🛠️ 해결방법**
- 쿠폰을 적용하는 것과 사용하는 개념은 다름.  
-> 적용은 장바구니에서, 사용은 주문단계에서 이뤄짐 
- 쿠폰 동시성 처리를 OrderServiceImpl에서 수행 

## **🚀 testConcurrentStockReduction**  
### **🔍 문제점**  
- 여러 스레드에서 동시에 주문 요청을 보내도 재고가 감소하지 않음.  
- 테스트가 종료되지 않거나 무한 대기 상태에 빠짐.  
- StockLock이 적용되었음에도 재고 값이 모든 스레드에서 동일하게 유지됨.

### **🛠️ 해결방법**  
1. productOption.decreaseQuantity(quantity); 실행 후에도 JPA 1차 캐시로 인해 변경 사항이 즉시 반영되지 않음.  
-> save() 대신 saveAndFlush()를 사용하여 트랜잭션이 끝나기 전에 즉시 변경 사항을 DB에 반영  
2. CountDownLatch 감소 (countDown())가 정상적으로 실행되지 않음  -> finally 블록에서 countDown() 실행
예외 발생 여부와 관계없이 countDownLatch.countDown();을 호출하여 모든 스레드가 정상적으로 종료되도록 수정

## **🚀 createSingleOrderCartItemWithMultipleUsers**  
### **🔍 문제점**  
- 여러 개의 스레드(사용자)가 동시에 같은 ProductOption(상품 옵션)에 대해 주문을 진행할 때,
재고가 정상적으로 감소하지 않음.
- @Transactional을 사용해도 동시성 문제가 해결되지 않음 => @Transactional(isolation = Isolation.SERIALIZABLE)을 적용해도 해결 X 

### **🛠️ 해결방법**    
PESSIMISTIC_WRITE(비관적 락) 적용  
하나의 스레드가 ProductOption을 조회하고 수정하는 동안 다른 트랜잭션이 접근하지 못하도록 설정  
@Lock(LockModeType.PESSIMISTIC_WRITE)을 적용하여 재고 감소 로직을 동기화  

✅ 기대 결과  
하나의 트랜잭션이 ProductOption을 수정하는 동안 다른 트랜잭션이 접근할 수 없음
동시에 재고 감소를 실행하려는 다른 스레드는 블록(blocking) 상태가 되어 순차적으로 실행됨
재고 초과 감소 방지 및 중복 실행 문제 해결  

## **🚀 applyDifferentCouponsAndCreateOrderConcurrently**

### 🔍 문제점
- 첫 번째 상품 주문에만 쿠폰이 적용되지 않는 문제 발생  
- 기존 방식에서는 쿠폰 적용과 주문 생성이 분리되어 있어, 쿠폰 적용이 완료되기 전에 주문이 실행됨  
- 멀티스레딩 환경에서 쿠폰 적용이 불완전한 상태에서 주문이 진행되어 일부 아이템이 쿠폰 없이 주문됨  
- 싱글 스레드(Single Thread) 방식에서는 쿠폰 적용 후 주문을 실행하므로 동기화 문제는 없지만, 속도가 느림  
- 멀티 스레드(Multi Thread) 방식에서는 성능은 개선되지만, 쿠폰이 적용되지 않은 상태에서 주문이 실행될 가능성이 있음

### 🛠️ 해결방법  
1. 쿠폰 적용과 주문 생성을 한 번에 처리  
   - 기존에는 쿠폰 적용 후 주문을 생성하는 방식이었지만, 쿠폰 적용과 주문 생성을 같은 스레드에서 동시에 진행하도록 변경  
   - ExecutorService.submit() 내에서 쿠폰 적용 → 검증 → 주문 실행이 한 번에 이루어지도록 수정  
2. 멀티스레드 환경에서 동시성 문제 해결  
   - threadCount = 10 설정하여 여러 개의 스레드에서 동시에 쿠폰 적용 및 주문 실행  
   - 각 스레드가 개별적으로 쿠폰 적용을 보장하도록 변경하여 첫 번째 주문에서도 쿠폰이 정상 적용됨  
3. 비관적 락(PESSIMISTIC_WRITE) 적용  
   - @Lock(LockModeType.PESSIMISTIC_WRITE)을 적용하여 하나의 쿠폰이 한 번만 사용되도록 보장  
   - 쿠폰 사용 여부를 동기화하여 중복 사용 방지  
4. 동시성 제어를 위한 CountDownLatch 제거  
   - 기존에는 CountDownLatch를 활용하여 쿠폰 적용 후 주문이 실행되도록 했지만, 쿠폰 적용과 주문이 동시에 실행되도록 변경하면서 필요 없어짐  
5. 재고 감소와 쿠폰 적용을 하나의 트랜잭션에서 처리  
   - @Transactional을 활용하여 쿠폰 적용과 재고 감소가 같은 트랜잭션에서 실행되도록 변경  
   - 주문 생성 중 예외 발생 시, 쿠폰 적용도 함께 롤백되도록 보장

✅ 기대 결과  
- 모든 카트 아이템에 쿠폰이 정상적으로 적용됨  
- 쿠폰 적용 후 즉시 주문이 생성되어 동기화 문제 발생 가능성이 낮아짐  
- 동시성 이슈로 인해 첫 번째 주문에서 쿠폰이 적용되지 않는 문제 해결  
- 멀티스레드 방식으로 처리하여 성능 최적화  
- 재고 부족으로 인해 주문 실패 시 쿠폰도 자동 롤백됨





