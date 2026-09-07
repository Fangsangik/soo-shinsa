package com.Soo_Shinsa.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합 테스트용 일회용 인프라.
 *
 * 기존에는 테스트가 개발 DB(localhost:3306)에 직접 써서 서로를 오염시켰고,
 * CI 에서는 아예 돌 수 없었다. 컨테이너를 띄워 격리한다.
 *
 * 컨테이너는 JVM 당 한 번만 띄우고 재사용한다(@Container 를 쓰면 클래스마다 재시작한다).
 * 종료는 Ryuk 이 처리하므로 stop 을 부르지 않는다.
 */
public abstract class IntegrationTestSupport {

    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("soo_shinsa")
                    .withUrlParam("serverTimezone", "Asia/Seoul")
                    .withUrlParam("characterEncoding", "UTF-8");

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    static {
        MYSQL.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        // 빈 DB 의 스키마는 Flyway 가 만든다. ddl-auto 는 운영과 같이 validate 로 두어
        // 마이그레이션이 엔티티와 어긋나면 테스트가 먼저 깨지게 한다.
        registry.add("spring.batch.jdbc.initialize-schema", () -> "always");
    }
}
