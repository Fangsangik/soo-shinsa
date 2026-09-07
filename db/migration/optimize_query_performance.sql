-- 🚀 SooShinsa 조회 성능 최적화를 위한 데이터베이스 인덱스

-- ===================================
-- 1. 기본 조회 성능 최적화 인덱스
-- ===================================

-- 주문 테이블 최적화 인덱스
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);
CREATE INDEX idx_orders_user_status ON orders(user_id, status);
CREATE INDEX idx_orders_user_status_created ON orders(user_id, status, created_at DESC);

-- 주문 아이템 테이블 최적화 인덱스
CREATE INDEX idx_order_items_order_status ON orderItems(orders_id, status);
CREATE INDEX idx_order_items_status_created ON orderItems(status, created_at);
CREATE INDEX idx_order_items_product_created ON orderItems(product_id, created_at DESC);

-- ===================================
-- 2. 커버링 인덱스 (성능 극대화)
-- ===================================

-- Orders 테이블 커버링 인덱스
CREATE INDEX idx_orders_covering_user_date ON orders(
    user_id, 
    created_at DESC, 
    id, 
    order_id, 
    total_price, 
    status
);

-- OrderItems 테이블 커버링 인덱스
CREATE INDEX idx_order_items_covering ON orderItems(
    orders_id, 
    status, 
    id, 
    product_id, 
    product_option_id, 
    quantity, 
    price, 
    discount_price
);

-- ===================================
-- 3. 부분 취소 관련 인덱스
-- ===================================

-- 취소 상태별 조회 최적화
CREATE INDEX idx_order_items_cancelled_at ON orderItems(cancelled_at) WHERE status IN ('CANCELLED', 'REFUNDED');
CREATE INDEX idx_order_items_status_reason ON orderItems(status, cancel_reason);

-- 부분 취소 통계 조회 최적화
CREATE INDEX idx_order_items_orders_status_amount ON orderItems(
    orders_id, 
    status, 
    COALESCE(discount_price, price),
    quantity
);

-- ===================================
-- 4. 조인 성능 최적화 인덱스
-- ===================================

-- Product 테이블 조인 최적화
CREATE INDEX idx_products_name_status ON products(product_name, product_status);

-- ProductOption 테이블 조인 최적화
CREATE INDEX idx_product_options_product_name ON product_options(product_id, option_name);

-- User 테이블 조인 최적화 (이미 있을 수 있음)
CREATE INDEX IF NOT EXISTS idx_users_id_status ON users(user_id, user_status);

-- ===================================
-- 5. 검색 및 필터링 최적화 인덱스
-- ===================================

-- 날짜 범위 검색 최적화
CREATE INDEX idx_orders_created_user ON orders(created_at DESC, user_id);
CREATE INDEX idx_orders_created_status ON orders(created_at DESC, status);

-- 주문 번호 검색 최적화
CREATE INDEX idx_orders_order_id_user ON orders(order_id, user_id);

-- 상품명 검색 최적화 (풀텍스트 인덱스)
CREATE FULLTEXT INDEX idx_products_fulltext_name ON products(product_name);

-- ===================================
-- 6. 성능 분석을 위한 뷰 최적화
-- ===================================

-- 기존 order_cancel_statistics 뷰 드롭
DROP VIEW IF EXISTS order_cancel_statistics;

-- 최적화된 부분 취소 통계 뷰 재생성
CREATE VIEW order_cancel_statistics AS
SELECT 
    o.id as order_id,
    o.order_id as order_number,
    o.user_id,
    o.status as order_status,
    o.created_at,
    COUNT(oi.id) as total_items,
    COUNT(CASE WHEN oi.status = 'ORDERED' THEN 1 END) as active_items,
    COUNT(CASE WHEN oi.status = 'CANCELLED' THEN 1 END) as cancelled_items,
    COUNT(CASE WHEN oi.status = 'REFUNDED' THEN 1 END) as refunded_items,
    COALESCE(SUM(CASE WHEN oi.status = 'ORDERED' 
                      THEN COALESCE(oi.discount_price, oi.price) * oi.quantity 
                      ELSE 0 END), 0) as active_amount,
    COALESCE(SUM(CASE WHEN oi.status IN ('CANCELLED', 'REFUNDED') 
                      THEN COALESCE(oi.discount_price, oi.price) * oi.quantity 
                      ELSE 0 END), 0) as cancelled_amount,
    CASE 
        WHEN COUNT(CASE WHEN oi.status = 'ORDERED' THEN 1 END) = 0 THEN 'FULLY_CANCELLED'
        WHEN COUNT(CASE WHEN oi.status IN ('CANCELLED', 'REFUNDED') THEN 1 END) > 0 THEN 'PARTIALLY_CANCELLED'
        ELSE 'ACTIVE'
    END as order_cancel_status
FROM orders o
LEFT JOIN orderItems oi ON o.id = oi.orders_id
GROUP BY o.id, o.order_id, o.user_id, o.status, o.created_at;

-- 뷰에 대한 인덱스 힌트를 위한 추가 인덱스
CREATE INDEX idx_orders_view_optimization ON orders(id, order_id, user_id, status, created_at);

-- ===================================
-- 7. 실시간 통계를 위한 집계 테이블
-- ===================================

-- 일별 주문 통계 테이블 (성능 향상용)
CREATE TABLE daily_order_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    date_key DATE NOT NULL,
    user_id BIGINT NOT NULL,
    total_orders INT DEFAULT 0,
    total_items INT DEFAULT 0,
    cancelled_orders INT DEFAULT 0,
    cancelled_items INT DEFAULT 0,
    total_amount DECIMAL(15,2) DEFAULT 0,
    cancelled_amount DECIMAL(15,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_daily_stats (date_key, user_id),
    INDEX idx_daily_stats_date (date_key),
    INDEX idx_daily_stats_user (user_id),
    INDEX idx_daily_stats_user_date (user_id, date_key)
);

-- ===================================
-- 8. 성능 모니터링용 인덱스
-- ===================================

-- 슬로우 쿼리 분석을 위한 추가 인덱스
CREATE INDEX idx_orders_performance_monitor ON orders(
    created_at, 
    user_id, 
    status, 
    id
) COMMENT '성능 모니터링용 복합 인덱스';

CREATE INDEX idx_order_items_performance_monitor ON orderItems(
    created_at, 
    orders_id, 
    status, 
    product_id
) COMMENT '성능 모니터링용 복합 인덱스';

-- ===================================
-- 9. 인덱스 사용 통계 및 최적화 확인
-- ===================================

-- 인덱스 사용률 확인을 위한 뷰 생성
CREATE VIEW index_usage_statistics AS
SELECT 
    s.table_schema,
    s.table_name,
    s.index_name,
    s.cardinality,
    st.rows_read,
    st.rows_examined
FROM information_schema.statistics s
LEFT JOIN information_schema.table_statistics st 
    ON s.table_schema = st.table_schema 
    AND s.table_name = st.table_name
WHERE s.table_schema = DATABASE()
    AND s.table_name IN ('orders', 'orderItems', 'products', 'product_options')
ORDER BY s.table_name, s.seq_in_index;

-- ===================================
-- 10. 파티셔닝 준비 (대용량 데이터 대비)
-- ===================================

-- 향후 파티셔닝을 위한 준비 (현재는 주석 처리)
/*
-- 월별 파티셔닝 예시 (대용량 데이터 시 적용)
ALTER TABLE orders PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
    PARTITION p202412 VALUES LESS THAN (202413),
    PARTITION p202501 VALUES LESS THAN (202502),
    PARTITION p202502 VALUES LESS THAN (202503),
    -- 매월 추가 필요
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
*/

-- ===================================
-- 최적화 확인 쿼리
-- ===================================

-- 생성된 인덱스 확인
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    CARDINALITY
FROM information_schema.statistics 
WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME IN ('orders', 'orderItems') 
    AND INDEX_NAME LIKE 'idx_%'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- 테이블 통계 정보 업데이트
ANALYZE TABLE orders;
ANALYZE TABLE orderItems;
ANALYZE TABLE products;
ANALYZE TABLE product_options;

-- 쿼리 캐시 초기화 (새로운 인덱스 반영)
FLUSH QUERY CACHE;