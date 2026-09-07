# 🔥 SooShinsa Grafana + Prometheus 실시간 모니터링 구축 가이드

## 📖 개요

Spring Boot 애플리케이션의 성능을 실시간으로 모니터링하기 위한 Grafana + Prometheus 환경을 Docker를 사용하여 구축하는 완전한 가이드입니다.

## 🏗️ 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Spring Boot   │────│   Prometheus    │────│    Grafana      │
│   Application   │    │   (메트릭 수집)  │    │  (대시보드)     │
│   :8080         │    │   :9090         │    │   :3000         │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
    /actuator/prometheus     메트릭 스크래핑        실시간 시각화
```

## ⚙️ 사전 준비사항

### 1. Spring Boot 애플리케이션 설정

#### build.gradle
```gradle
// Monitoring & Metrics (Prometheus, Grafana)
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

#### application.properties
```properties
# Actuator & Prometheus Metrics
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.endpoint.metrics.enabled=true
management.endpoint.prometheus.enabled=true
management.metrics.export.prometheus.enabled=true
management.metrics.distribution.percentiles-histogram.http.server.requests=true
management.metrics.tags.application=soo-shinsa
```

#### 화이트리스트 설정 (UrlConst.java)
```java
public static final String[] WHITE_LIST = {
    "/users/login", "/users/signin", "/users/logout", 
    "/v3/api-docs/**", "/oauth2/**", "/auth/**", 
    "/swagger-ui/**", "/swagger-ui.html", "/api/v1/users", 
    "/kakao/callback", "/api/chat/**", "/ws/**", 
    "/test", "/stylesheets/**", "/success", 
    "/actuator/**", "/actuator/health"  // 추가된 부분
};
```

### 2. 필수 도구 설치
- Docker Desktop
- Apache JMeter 5.6.3+

## 🚀 설치 및 구축 과정

### 1단계: Docker Compose 설정

#### docker-compose.yml
```yaml
services:
  prometheus:
    image: prom/prometheus:latest
    container_name: soo-shinsa-prometheus
    ports:
      - "9090:9090"
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=200h'
      - '--web.enable-lifecycle'
    volumes:
      - prometheus_data:/prometheus
    extra_hosts:
      - "host.docker.internal:host-gateway"
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    container_name: soo-shinsa-grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin123
      - GF_SECURITY_ADMIN_USER=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana_data:/var/lib/grafana
    depends_on:
      - prometheus
    restart: unless-stopped

volumes:
  prometheus_data:
  grafana_data:
```

### 2단계: 서비스 시작

#### start-monitoring.sh
```bash
#!/bin/bash

echo "🔥 SooShinsa 실시간 성능 모니터링 시작"
echo "====================================="

# Docker Compose 실행
echo "📊 Prometheus + Grafana 컨테이너 시작 중..."
docker-compose up -d

# 서비스 상태 확인
echo ""
echo "⏳ 서비스 시작 대기 중..."
sleep 10

# Prometheus 상태 확인
echo "🔍 Prometheus 상태 확인..."
if curl -f http://localhost:9090/-/healthy > /dev/null 2>&1; then
    echo "✅ Prometheus 실행 중: http://localhost:9090"
else
    echo "❌ Prometheus 실행 실패"
fi

# Grafana 상태 확인
echo "🔍 Grafana 상태 확인..."
if curl -f http://localhost:3000/api/health > /dev/null 2>&1; then
    echo "✅ Grafana 실행 중: http://localhost:3000"
    echo "   • 사용자: admin"
    echo "   • 비밀번호: admin123"
else
    echo "❌ Grafana 실행 실패"
fi

# Spring Boot 앱 상태 확인
echo "🔍 Spring Boot 앱 상태 확인..."
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ Spring Boot 앱 실행 중: http://localhost:8080"
    echo "   • Prometheus 메트릭: http://localhost:8080/actuator/prometheus"
else
    echo "❌ Spring Boot 앱이 실행되지 않음"
    echo "   → 먼저 Spring Boot 앱을 실행하세요"
fi
```

### 3단계: Prometheus 설정

```bash
# Prometheus 컨테이너에 설정 추가
docker exec soo-shinsa-prometheus sh -c 'cat > /etc/prometheus/prometheus.yml << EOF
global:
  scrape_interval: 1s
  evaluation_interval: 1s

scrape_configs:
  - job_name: "soo-shinsa-app"
    static_configs:
      - targets: ["host.docker.internal:8080"]
    metrics_path: "/actuator/prometheus"
    scrape_interval: 1s
    scrape_timeout: 1s

  - job_name: "prometheus"
    static_configs:
      - targets: ["localhost:9090"]
EOF'

# 설정 리로드
docker exec soo-shinsa-prometheus kill -HUP 1
```

### 4단계: Grafana 데이터소스 설정

```bash
# Prometheus 데이터소스 추가
curl -X POST -H "Content-Type: application/json" -d '{
  "name": "Prometheus",
  "type": "prometheus",
  "url": "http://prometheus:9090",
  "access": "proxy",
  "isDefault": true
}' http://admin:admin123@localhost:3000/api/datasources
```

### 5단계: 대시보드 생성

#### create-dashboard.sh
```bash
#!/bin/bash

echo "📊 Grafana 대시보드 생성 중..."

DASHBOARD_JSON='{
  "dashboard": {
    "id": null,
    "title": "🔥 SooShinsa 실시간 TPS 모니터링",
    "tags": ["performance", "jmeter", "spring-boot"],
    "timezone": "browser",
    "refresh": "1s",
    "time": {
      "from": "now-5m",
      "to": "now"
    },
    "panels": [
      {
        "id": 1,
        "title": "🚀 실시간 TPS",
        "type": "stat",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{application=\"soo-shinsa\"}[10s])",
            "legendFormat": "TPS",
            "refId": "A"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "reqps",
            "min": 0,
            "decimals": 1,
            "thresholds": {
              "steps": [
                {"color": "red", "value": 0},
                {"color": "yellow", "value": 20},
                {"color": "green", "value": 50},
                {"color": "super-light-green", "value": 80}
              ]
            }
          }
        },
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "⚡ 평균 응답시간",
        "type": "stat",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_sum{application=\"soo-shinsa\"}[10s]) / rate(http_server_requests_seconds_count{application=\"soo-shinsa\"}[10s]) * 1000",
            "legendFormat": "평균 응답시간",
            "refId": "A"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "ms",
            "min": 0,
            "decimals": 1,
            "thresholds": {
              "steps": [
                {"color": "green", "value": 0},
                {"color": "yellow", "value": 100},
                {"color": "red", "value": 500}
              ]
            }
          }
        },
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0}
      },
      {
        "id": 3,
        "title": "📈 TPS 실시간 그래프",
        "type": "timeseries",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{application=\"soo-shinsa\"}[10s])",
            "legendFormat": "TPS",
            "refId": "A"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "reqps",
            "min": 0
          }
        },
        "gridPos": {"h": 8, "w": 24, "x": 0, "y": 8}
      }
    ]
  },
  "message": "SooShinsa 실시간 성능 모니터링 대시보드",
  "overwrite": true
}'

# 대시보드 생성
curl -X POST \
  -H "Content-Type: application/json" \
  -d "$DASHBOARD_JSON" \
  http://admin:admin123@localhost:3000/api/dashboards/db

echo ""
echo "✅ 대시보드 생성 완료!"
echo "🌐 Grafana: http://localhost:3000"
echo "👤 사용자: admin"
echo "🔐 비밀번호: admin123"
```

## 🎯 JMeter 부하 테스트 설정

### 현재 JMeter 구성

#### Duration & 동시성 설정
```xml
<!-- 기본 변수 설정 -->
<elementProp name="THREADS" elementType="Argument">
    <stringProp name="Argument.value">100</stringProp>
</elementProp>
<elementProp name="RAMP_UP" elementType="Argument">
    <stringProp name="Argument.value">10</stringProp>
</elementProp>
<elementProp name="DURATION" elementType="Argument">
    <stringProp name="Argument.value">60</stringProp>
</elementProp>
<elementProp name="LOOPS" elementType="Argument">
    <stringProp name="Argument.value">10</stringProp>
</elementProp>
```

#### Timeout 설정
```xml
<stringProp name="HTTPSampler.connect_timeout">10000</stringProp>  <!-- 10초 -->
<stringProp name="HTTPSampler.response_timeout">30000</stringProp> <!-- 30초 -->
```

#### Retry 정책
```xml
<stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
```

### 실시간 모니터링 부하 테스트

#### run-realtime-monitoring.sh
```bash
#!/bin/bash

echo "🔥 실시간 Grafana 모니터링 부하 테스트 시작!"

# 시나리오별 테스트 실행
SCENARIOS=(100 200 300 500)

for threads in "${SCENARIOS[@]}"; do
    echo "🧪 ${threads}명 실시간 모니터링 테스트 시작!"
    
    # JMeter 백그라운드 실행
    jmeter -n -t coupon-stock-concurrency-test.jmx \
        -JSERVER_URL=http://localhost:8080 \
        -JTHREADS=${threads} \
        -JRAMP_UP=30 \
        -JDURATION=120 \
        -JLOOPS=30 \
        -l "results/realtime-${threads}.jtl" \
        > "results/realtime-${threads}.log" 2>&1 &
    
    echo "⏳ 2분 30초 진행 중... (Grafana에서 실시간 확인!)"
    sleep 150
    
    echo "✅ ${threads}명 테스트 완료!"
    sleep 30  # 시스템 안정화
done
```

## 📊 모니터링 지표 해석

### 🚀 핵심 지표

#### 1. TPS (Transaction Per Second)
- **측정**: `rate(http_server_requests_seconds_count[10s])`
- **기준값**:
  - 🥇 80+ req/s: 최고 성능
  - 🥈 50+ req/s: 우수 성능
  - 🥉 20+ req/s: 양호 성능
  - ⚠️ 20 미만: 성능 개선 필요

#### 2. 평균 응답시간
- **측정**: `rate(http_server_requests_seconds_sum[10s]) / rate(http_server_requests_seconds_count[10s]) * 1000`
- **기준값**:
  - 🟢 0-50ms: 우수
  - 🟡 50-100ms: 양호
  - 🔴 100ms+: 주의 필요

#### 3. HTTP 상태별 요청
- **성공**: `rate(http_server_requests_seconds_count{status="200"}[10s])`
- **오류**: `rate(http_server_requests_seconds_count{status!="200"}[10s])`

### 📈 성능 분석 기준

#### 정상 상태
- TPS: 50-100 req/s
- 응답시간: 50ms 이하
- 성공률: 99.5% 이상
- 그래프: 안정적 수평선

#### 주의 상태
- TPS: 급격한 하락
- 응답시간: 100ms 이상
- 성공률: 95-99% 
- 그래프: 변동폭 증가

#### 위험 상태
- TPS: 20 미만
- 응답시간: 500ms 이상
- 성공률: 95% 미만
- 그래프: 불규칙한 패턴

## 🛠️ 사용법

### 1. 모니터링 환경 시작
```bash
# 실행 권한 부여
chmod +x start-monitoring.sh create-dashboard.sh run-realtime-monitoring.sh

# 모니터링 시작
./start-monitoring.sh

# 대시보드 생성
./create-dashboard.sh
```

### 2. Grafana 접속
- **URL**: http://localhost:3000
- **사용자**: admin
- **비밀번호**: admin123

### 3. 실시간 부하 테스트 실행
```bash
./run-realtime-monitoring.sh
```

### 4. 결과 확인
- **실시간 모니터링**: Grafana 대시보드
- **상세 결과**: `results/realtime-*.jtl` 파일
- **로그**: `results/realtime-*.log` 파일

## 🔧 고급 설정

### Prometheus 메트릭 수집 간격 조정
```yaml
# prometheus.yml
global:
  scrape_interval: 1s    # 1초마다 수집 (고빈도)
  scrape_interval: 5s    # 5초마다 수집 (일반)
  scrape_interval: 15s   # 15초마다 수집 (저빈도)
```

### Grafana 새로고침 간격
```json
{
  "refresh": "1s",   // 1초마다 새로고침 (실시간)
  "refresh": "5s",   // 5초마다 새로고침 (준실시간)
  "refresh": "30s"   // 30초마다 새로고침 (일반)
}
```

### JMeter 파라미터 동적 조정
```bash
# 극한 성능 테스트
jmeter -n -t test.jmx -JTHREADS=500 -JDURATION=300 -JRAMP_UP=60

# 안정성 테스트  
jmeter -n -t test.jmx -JTHREADS=200 -JDURATION=600 -JRAMP_UP=120

# 빠른 검증 테스트
jmeter -n -t test.jmx -JTHREADS=100 -JDURATION=60 -JRAMP_UP=10
```

## ⚠️ 주의사항

### Docker 파일 공유 설정
- macOS: Docker Desktop → Preferences → Resources → File Sharing
- 프로젝트 디렉토리가 공유 목록에 포함되어야 함

### 메모리 사용량
- Prometheus: 최소 1GB RAM
- Grafana: 최소 512MB RAM
- JMeter: 동시 사용자 수에 따라 가변

### 네트워크 포트
- 3000: Grafana
- 8080: Spring Boot
- 9090: Prometheus

## 🚀 성능 최적화 팁

### 1. Prometheus 설정
```yaml
# 데이터 보존 기간 단축 (디스크 절약)
command:
  - '--storage.tsdb.retention.time=24h'
```

### 2. Grafana 대시보드
```json
{
  "time": {
    "from": "now-1m",    // 1분간 데이터만 표시 (성능 향상)
    "to": "now"
  }
}
```

### 3. JMeter 최적화
```bash
# JVM 힙 메모리 증가
export JVM_ARGS="-Xms1g -Xmx4g"
jmeter -n -t test.jmx
```

## 🛑 서비스 종료

```bash
# 모니터링 서비스 종료
docker-compose down

# 데이터 볼륨까지 삭제
docker-compose down -v

# 시스템 정리
docker system prune -f
```

## 📝 트러블슈팅

### 1. Prometheus 메트릭이 수집되지 않는 경우
```bash
# Spring Boot 메트릭 엔드포인트 확인
curl http://localhost:8080/actuator/prometheus

# Prometheus 타겟 상태 확인
curl http://localhost:9090/api/v1/targets
```

### 2. Grafana 대시보드가 비어있는 경우
```bash
# 데이터소스 연결 확인
curl http://admin:admin123@localhost:3000/api/datasources

# Prometheus 쿼리 직접 테스트
curl 'http://localhost:9090/api/v1/query?query=http_server_requests_seconds_count'
```

### 3. JMeter 테스트 실패 시
```bash
# JMeter 로그 확인
tail -f results/realtime-*.log

# 애플리케이션 상태 확인
curl http://localhost:8080/actuator/health
```

## 🎉 완성된 환경

✅ **Prometheus**: 실시간 메트릭 수집  
✅ **Grafana**: 시각적 대시보드  
✅ **JMeter**: 부하 테스트 자동화  
✅ **Spring Boot**: 성능 메트릭 제공  
✅ **실시간 모니터링**: TPS, 응답시간, 성공률

이제 **실시간으로 TPS 변화를 눈으로 확인**하면서 성능 테스트를 수행할 수 있습니다! 🔥