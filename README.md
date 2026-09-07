# SooShinsa (수신사)

브랜드 입점형 이커머스 서버. 회원/브랜드/상품/장바구니/주문/결제/쿠폰/리뷰/통계로 구성돼 있고,
선착순 쿠폰과 재고 차감처럼 **동시에 들어오면 틀어지는 지점**을 어떻게 막았는지가 이 프로젝트의 중심입니다.

## 기술 스택

| 영역 | 사용 |
|---|---|
| 언어 / 프레임워크 | Java 17, Spring Boot 3.3.2 |
| 데이터 | MySQL 8, Redis 7, JPA + QueryDSL + MyBatis |
| 스키마 | Flyway (`ddl-auto=validate`) |
| 인증 | Spring Security, JWT, Kakao OAuth2 |
| 결제 | Toss Payments |
| 배치 | Spring Batch (일 단위 매출 통계) |
| 관측 | Actuator + Micrometer + Prometheus |
| 테스트 | JUnit 5, Mockito, Testcontainers (MySQL/Redis) |

## 실행

MySQL 8과 Redis 7이 필요합니다. 전체를 컨테이너로 띄우려면:

```bash
docker compose up -d          # mysql, redis, app, prometheus, grafana, nginx
```

로컬에서 앱만 직접 띄우려면 MySQL/Redis를 올린 뒤:

```bash
./gradlew bootRun             # http://localhost:8080, Swagger: /swagger-ui.html
```

REST API는 모두 `/api/v1` 아래에 있습니다(`ApiPathConfig`가 컨트롤러에 접두사를 붙입니다).
예외는 토스가 직접 호출하는 결제 콜백 `/api/success`, `/api/fail` 두 개뿐입니다.

스키마는 Flyway가 만듭니다(`src/main/resources/db/migration`). 이미 스키마가 있는 DB는
`baseline-on-migrate`로 V1을 건너뜁니다. 데모용 시드 데이터는 `SEED_DATA=false`로 끕니다.

민감값은 모두 환경변수입니다: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
`ADMIN_SECRET_KEY`, `TOSS_SECRET_KEY`, `TOSS_CLIENT_KEY`, `AWS_*`.

## 테스트

```bash
./gradlew test                # Docker 필요 (Testcontainers가 MySQL/Redis를 띄웁니다)
```

테스트는 개발 DB를 건드리지 않고 일회용 컨테이너에서 돕니다. 컨테이너 스키마도 Flyway가
만들고 Hibernate는 `validate`로 두기 때문에, **마이그레이션과 엔티티가 어긋나면 테스트가 먼저 깨집니다.**

## 해결한 문제

### 선착순 쿠폰이 정원을 넘겨 발급되던 문제

분산 락을 걸었지만 락 해제가 트랜잭션 커밋보다 먼저 일어나서, 정원 10장짜리 쿠폰이 **20장** 나갔습니다.
락을 걷어내고 조건부 원자 UPDATE(`WHERE issued_count < max_count`)를 DB의 진실로 삼은 뒤,
Redis 선차단(SETNX + DECR)을 앞단에 두어 정원 초과 요청이 DB까지 오지 않게 했습니다.
Redis가 죽으면 fail-open으로 통과시키고 DB가 막습니다.

### 주문해도 재고가 줄지 않던 문제

락 키의 SpEL이 평가되지 않아 락이 사실상 없었고, 재고 차감 자체가 빠져 있었습니다.
`UPDATE ... SET quantity = quantity - :n WHERE id = :id AND quantity >= :n` 한 문장으로 바꾸고,
장바구니 주문은 `productOptionId` 순으로 정렬해 데드락을 피합니다.

### 측정값 (같은 장비, 서비스 계층, 64스레드, 3회 median)

| 시나리오 | 개선 전 | 개선 후 |
|---|---|---|
| 쿠폰 정원 10 / 요청 2000 — 발급 수 | **20장** (정원 초과) | **10장** |
| — 처리량 | 1,943 req/s | **3,278 req/s** (+68.7%) |
| 재고 200 / 주문 200 — 남은 재고 | **200** (차감 안 됨) | **0** |
| — 처리량 | 210 req/s | **275 req/s** (+31.0%) |

Redis 선차단 A/B: OFF 2,597 → ON 3,710 req/s (+43%).

### 그 밖에

- 브랜드 관리자 API가 인증 없이 열려 있었습니다(`@PreAuthorize`가 `@EnableMethodSecurity` 없이 무력화). 필터 체인 규칙으로 막고 HTTP 계층 회귀 테스트를 붙였습니다.
- 결제 취소 경로가 둘인데 한쪽만 재고를 되돌렸습니다. `OrderCancellationService`로 합쳤습니다.
- 결제사 API 호출이 트랜잭션 안에 있어 네트워크 대기 동안 DB 커넥션을 물고 있었습니다. `verify → call → apply`로 분리했습니다.
- 미결제 주문이 재고를 영구히 잡고 있었습니다. `PendingOrderSweeper`가 만료된 PENDING 주문을 취소하고 재고를 돌려놓습니다.
- Spring Batch 메타데이터 테이블이 없어 매일 00:00 통계 배치가 실패하고 있었습니다.
- 결제 콜백이 존재하지 않는 Thymeleaf 뷰 이름을 반환해 승인 직후 500으로 끝났습니다. 결과를 붙여 화면으로 돌려보냅니다.

자세한 과정은 [docs/troubleshooting.md](docs/troubleshooting.md),
[docs/lock-strategy-improvement.md](docs/lock-strategy-improvement.md),
[docs/coupon-concurrency-test.md](docs/coupon-concurrency-test.md),
[docs/performance-test.md](docs/performance-test.md),
[docs/query-optimization.md](docs/query-optimization.md)에 있습니다.

## 프론트엔드

`src/main/resources/static/index.html` 한 장이 앱이 서빙하는 화면입니다(빌드 단계 없음).
브랜드 목록, 상품 검색·자동완성, 결제 결과 표시가 들어 있습니다.

`frontend/`는 같은 도메인을 모듈로 쪼갠 별도 화면(로그인, 브랜드 승인 대시보드)입니다.
서버가 서빙하지 않으므로 `frontend/index.html`을 브라우저로 직접 열어서 씁니다.
API 주소는 `frontend/js/config.js`의 `BASE_URL`에 있습니다.
