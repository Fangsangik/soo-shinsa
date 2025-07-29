# SooShinsa (수신사) 🛍️

> **문제 해결 중심의 실전형 이커머스 플랫폼**  
> 동시성 충돌, 데이터 정합성, 성능 병목 등 실제 운영 환경에서 발생하는 복잡한 문제를 해결한 프로젝트

## 📌 프로젝트 개요

**SooShinsa**는 쿠팡 및 무신사와 같은 이커머스를 모티브로 한 온라인 쇼핑몰 프로젝트입니다. 회원가입 및 백오피스 기능을 포함하여, 회원 등급에 따라 포인트 적립, 할인, 포인트 사용이 가능하며, 관리자와 점주는 매출 및 판매 현황을 분석하여 운영 효율성을 높일 수 있습니다. 소비자는 브랜드별 카테고리에서 상품을 탐색 및 구매할 수 있으며, 쿠폰 시스템을 통해 추가적인 할인 혜택을 누릴 수 있습니다.

## 🛠️ 기술 스택

- **Backend**: Java, Spring Boot, JPA, QueryDSL, Socket.IO
- **Database**: MySQL, Redis
- **Infra**: AWS (S3, EC2, RDS)
- **API Management**: Postman, Swagger
- **Payment**: Toss Payments
- **Authentication**: SpringSecurity, Kakao OAuth2
- **CORS**: Simple global configuration so the React frontend can call the API

## 🌐 Frontend

The project ships with a small React based UI under `frontend`. Open `frontend/index.html` directly in your browser and you will be able to log in with your user account and navigate through several screens. Navigation is handled via React Router so no build step is required. From the UI you can look up products, browse categories and brands, view your cart and your past orders.

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

## 🔧 **최근 코드 품질 개선 및 성능 최적화**

### 🚨 **1. 보안 취약점 해결**
- **민감정보 환경변수 분리**: JWT Secret, AWS 키, 결제 API 키 분리
- **CORS 보안 강화**: 특정 도메인만 허용하도록 변경
- **JWT 로그아웃 수정**: 현재 사용자 토큰만 삭제하도록 개선

### ⚡ **2. 성능 문제 해결**
- **N+1 Query 문제 해결**: Fetch Join으로 한번에 조회
- **CartItem 성능 최적화**: 연관 엔티티 한 번에 조회

### 🔒 **3. 분산락 + DB락 데드락 문제 해결**
- **락 전략 단순화**: 분산락만 사용, DB락 제거
- **격리 수준 최적화**: SERIALIZABLE → READ_COMMITTED
- **원자적 업데이트**: 경쟁 조건 방지를 위한 단일 쿼리 사용

### 📊 **4. 성능 테스트 환경 구축**
- **JMeter 테스트**: 동시 사용자 100명, 60초 지속
- **성능 개선**: TPS 273 → 546 (오류율 50% → 1.43%)

---

## 📚 **추가 문서**

상세한 트러블슈팅 내용은 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)를 참고하세요.
