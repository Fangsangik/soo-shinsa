# 📊 Grafana 대시보드 설정 가이드

## 🚀 Grafana 실행 방법

### 1️⃣ Docker 파일 공유 설정

**macOS Docker Desktop에서:**
1. Docker Desktop 실행
2. 상단 메뉴 Docker → Settings (또는 Preferences)
3. Resources → File Sharing 메뉴 이동
4. 다음 경로 추가:
   ```
   /Users/hwangsang-ik/IdeaProjects/sparta/soo-shinsa/performance-test
   ```
5. "Apply & restart" 클릭

### 2️⃣ Grafana + Prometheus 스택 실행

```bash
cd performance-test
docker-compose -f docker-compose-monitoring.yml up -d
```

### 3️⃣ 접속 정보

| 서비스 | URL | 계정 |
|--------|-----|------|
| **Grafana** | http://localhost:3000 | admin/admin |
| **Prometheus** | http://localhost:9090 | 없음 |
| **SooShinsa 메트릭** | http://localhost:8080/actuator/prometheus | 없음 |

### 4️⃣ Grafana 대시보드 설정

#### 자동 설정 (권장)
- 이미 설정된 대시보드가 자동으로 로드됩니다
- `monitoring/grafana/provisioning/dashboards/soo-shinsa-performance.json`

#### 수동 설정
1. Grafana 접속 → Dashboards → Import
2. 대시보드 JSON 파일 업로드
3. Prometheus 데이터소스 선택

## 📈 주요 메트릭 시각화

### 1. TPS (Transactions Per Second)
```promql
rate(http_server_requests_seconds_count[1m])
```

### 2. 응답시간 분포
```promql
# P50 (중간값)
histogram_quantile(0.50, rate(http_server_requests_seconds_bucket[1m]))

# P95 (95% 백분위)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[1m]))

# P99 (99% 백분위)
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[1m]))
```

### 3. 성공률
```promql
sum(rate(http_server_requests_seconds_count{status!~"5.."}[1m])) / 
sum(rate(http_server_requests_seconds_count[1m])) * 100
```

### 4. JVM 메모리 사용률
```promql
jvm_memory_used_bytes / jvm_memory_max_bytes * 100
```

### 5. 데이터베이스 커넥션 풀
```promql
# 활성 커넥션
hikaricp_connections_active

# 유휴 커넥션
hikaricp_connections_idle

# 최대 커넥션
hikaricp_connections_max
```

### 6. 시스템 CPU 사용률
```promql
100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[1m])) * 100)
```

## 🎯 실시간 모니터링 활용법

### 1. JMeter 테스트 중 모니터링
```bash
# 터미널 1: JMeter 테스트 실행
./run-progressive-load-test.sh

# 터미널 2: Grafana 대시보드 확인
open http://localhost:3000
```

### 2. 알림 설정
- **TPS 임계값**: < 25 TPS
- **응답시간 임계값**: > 200ms
- **오류율 임계값**: > 5%
- **메모리 사용률**: > 80%

### 3. 대시보드 패널 구성
```
┌─────────────┬─────────────┬─────────────┐
│     TPS     │  응답시간   │   성공률    │
├─────────────┼─────────────┼─────────────┤
│    TPS vs 시간 차트 (실시간)      │
├─────────────────────────────────────┤
│    응답시간 분포 (P50, P95, P99)   │
├─────────────┬─────────────────────────┤
│ DB 커넥션 풀│     시스템 CPU/메모리    │
└─────────────┴─────────────────────────┘
```

## 🔧 트러블슈팅

### Docker 연결 오류
```bash
# Docker 서비스 확인
docker ps

# 로그 확인
docker-compose logs grafana
docker-compose logs prometheus
```

### 메트릭 수집 안됨
```bash
# SooShinsa 메트릭 엔드포인트 확인
curl http://localhost:8080/actuator/prometheus

# Prometheus 타겟 상태 확인
# http://localhost:9090/targets
```

### 대시보드 로드 실패
```bash
# Grafana 데이터소스 확인
# http://localhost:3000/datasources

# Prometheus 연결 테스트
# Connection → Save & Test 버튼 클릭
```

## 📊 대시보드 스크린샷 예시

### TPS 모니터링
- **실시간 TPS**: 선 그래프로 시간별 변화 추적
- **스레드별 TPS**: 막대 그래프로 동시 사용자 수별 성능 비교

### 응답시간 분석
- **P50/P95/P99**: 백분위별 응답시간 분포
- **평균 vs 최대**: 응답시간 변동성 확인

### 시스템 상태
- **CPU 사용률**: 서버 부하 상태
- **메모리 사용률**: JVM 힙 메모리 추적
- **DB 커넥션**: 데이터베이스 병목 확인

## 🎉 실행 예시

```bash
# 1. Grafana 스택 시작
docker-compose -f docker-compose-monitoring.yml up -d

# 2. 브라우저에서 Grafana 접속
open http://localhost:3000

# 3. 성능 테스트 실행
./run-progressive-load-test.sh

# 4. 실시간 모니터링 대시보드에서 TPS 변화 관찰
# - 스레드 수 증가에 따른 TPS 변화
# - 응답시간 패턴 분석
# - 시스템 리소스 사용량 추적
```

이제 **실시간으로 TPS와 성능 지표를 시각적으로 모니터링**할 수 있습니다! 🚀