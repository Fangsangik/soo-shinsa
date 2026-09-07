-- 주문 아이템 부분 취소 기능을 위한 테이블 구조 변경

-- OrderItem 테이블에 상태, 취소 시간, 취소 사유 컬럼 추가
ALTER TABLE orderItems 
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ORDERED',
ADD COLUMN cancelled_at TIMESTAMP NULL,
ADD COLUMN cancel_reason VARCHAR(500) NULL;

-- 상태 컬럼에 체크 제약 조건 추가
ALTER TABLE orderItems 
ADD CONSTRAINT chk_order_item_status 
CHECK (status IN ('ORDERED', 'CANCELLED', 'REFUNDED'));

-- 기존 데이터에 대한 기본값 설정
UPDATE orderItems 
SET status = 'ORDERED' 
WHERE status IS NULL;

-- 인덱스 추가 (성능 최적화)
CREATE INDEX idx_order_item_status ON orderItems(status);
CREATE INDEX idx_order_item_cancelled_at ON orderItems(cancelled_at);

-- 부분 취소 관련 통계를 위한 뷰 생성
CREATE VIEW order_cancel_statistics AS
SELECT 
    o.id as order_id,
    o.order_id as order_number,
    COUNT(oi.id) as total_items,
    COUNT(CASE WHEN oi.status = 'ORDERED' THEN 1 END) as active_items,
    COUNT(CASE WHEN oi.status = 'CANCELLED' THEN 1 END) as cancelled_items,
    COUNT(CASE WHEN oi.status = 'REFUNDED' THEN 1 END) as refunded_items,
    SUM(CASE WHEN oi.status = 'ORDERED' THEN COALESCE(oi.discount_price, oi.price) * oi.quantity ELSE 0 END) as active_amount,
    SUM(CASE WHEN oi.status IN ('CANCELLED', 'REFUNDED') THEN COALESCE(oi.discount_price, oi.price) * oi.quantity ELSE 0 END) as cancelled_amount,
    CASE 
        WHEN COUNT(CASE WHEN oi.status = 'ORDERED' THEN 1 END) = 0 THEN 'FULLY_CANCELLED'
        WHEN COUNT(CASE WHEN oi.status IN ('CANCELLED', 'REFUNDED') THEN 1 END) > 0 THEN 'PARTIALLY_CANCELLED'
        ELSE 'ACTIVE'
    END as order_cancel_status
FROM orders o
LEFT JOIN orderItems oi ON o.id = oi.orders_id
GROUP BY o.id, o.order_id;

-- 부분 취소 히스토리 추적을 위한 테이블 생성
CREATE TABLE order_item_cancel_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    original_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    cancel_reason VARCHAR(500),
    cancelled_by BIGINT,
    cancelled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (order_item_id) REFERENCES orderItems(id),
    FOREIGN KEY (cancelled_by) REFERENCES users(user_id)
);

-- 취소 히스토리 인덱스
CREATE INDEX idx_cancel_history_order_item ON order_item_cancel_history(order_item_id);
CREATE INDEX idx_cancel_history_cancelled_at ON order_item_cancel_history(cancelled_at);

-- 부분 취소 트리거 (선택사항: 취소 히스토리 자동 기록)
DELIMITER $$

CREATE TRIGGER order_item_status_change_trigger
    AFTER UPDATE ON orderItems
    FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status AND NEW.status IN ('CANCELLED', 'REFUNDED') THEN
        INSERT INTO order_item_cancel_history (
            order_item_id, 
            original_status, 
            new_status, 
            cancel_reason,
            cancelled_at
        ) VALUES (
            NEW.id, 
            OLD.status, 
            NEW.status, 
            NEW.cancel_reason,
            NEW.cancelled_at
        );
    END IF;
END$$

DELIMITER ;

-- 성능 최적화를 위한 추가 인덱스
CREATE INDEX idx_order_items_composite ON orderItems(orders_id, status);
CREATE INDEX idx_orders_status ON orders(status);

-- 코멘트 추가
ALTER TABLE orderItems 
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ORDERED' 
COMMENT '주문 아이템 상태: ORDERED(주문됨), CANCELLED(취소됨), REFUNDED(환불됨)';

ALTER TABLE orderItems 
MODIFY COLUMN cancelled_at TIMESTAMP NULL 
COMMENT '취소 일시';

ALTER TABLE orderItems 
MODIFY COLUMN cancel_reason VARCHAR(500) NULL 
COMMENT '취소 사유';

-- 데이터 무결성을 위한 추가 제약조건
ALTER TABLE orderItems 
ADD CONSTRAINT chk_cancelled_at_with_status 
CHECK (
    (status = 'ORDERED' AND cancelled_at IS NULL) OR 
    (status IN ('CANCELLED', 'REFUNDED') AND cancelled_at IS NOT NULL)
);