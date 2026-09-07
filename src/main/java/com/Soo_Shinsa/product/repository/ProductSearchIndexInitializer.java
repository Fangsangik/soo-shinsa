package com.Soo_Shinsa.product.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 상품명 FULLTEXT 인덱스 생성.
 *
 * 자동완성은 LIKE '%키워드%' 라 인덱스를 타지 못하고 매번 전체 스캔이었다.
 * Hibernate 의 ddl-auto 는 FULLTEXT 인덱스를 만들지 못하므로 여기서 직접 만든다.
 * 이미 있으면 아무것도 하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.search.fulltext-index-enabled", havingValue = "true", matchIfMissing = true)
public class ProductSearchIndexInitializer implements ApplicationRunner {

    public static final String INDEX_NAME = "ft_product_name";

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer existing = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                    "WHERE table_schema = DATABASE() AND table_name = 'product' AND index_name = ?",
                    Integer.class, INDEX_NAME);

            if (existing != null && existing > 0) {
                return;
            }

            // ngram 파서를 써야 한글 부분일치가 된다 (기본 파서는 공백 단위)
            jdbc.execute("ALTER TABLE product ADD FULLTEXT INDEX " + INDEX_NAME + " (name) WITH PARSER ngram");
            log.info("상품명 FULLTEXT 인덱스 생성 완료: {}", INDEX_NAME);
        } catch (Exception e) {
            // 인덱스가 없으면 자동완성 쿼리가 실패하므로 조용히 넘기지 않는다
            throw new IllegalStateException("상품명 FULLTEXT 인덱스를 만들지 못했습니다: " + e.getMessage(), e);
        }
    }
}
