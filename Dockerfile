# 🐳 SooShinsa Spring Boot Application Dockerfile
# 멀티스테이지 빌드로 이미지 크기 최적화

# Build Stage
FROM gradle:8.5-jdk21 AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 캐시 최적화를 위해 의존성 파일 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 레이어)
RUN gradle dependencies --no-daemon

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드 (테스트 제외)
RUN gradle build -x test --no-daemon

# Runtime Stage
FROM openjdk:21-jre-slim

# 보안을 위한 비루트 사용자 생성
RUN groupadd -r sooshinsa && useradd -r -g sooshinsa sooshinsa

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 로그 디렉토리 생성 및 권한 설정
RUN mkdir -p /app/logs && chown -R sooshinsa:sooshinsa /app

# 비루트 사용자로 전환
USER sooshinsa

# 포트 노출
EXPOSE 8080

# 메트릭 포트 노출 (Prometheus)
EXPOSE 9090

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM 최적화 옵션
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+UseContainerSupport"

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]