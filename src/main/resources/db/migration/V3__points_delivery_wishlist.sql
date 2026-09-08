-- 포인트/등급 승급, 배송 상태, 찜.

-- 포인트 잔액과 누적 구매액. 등급(grade.requirement)은 누적 구매액으로 판정한다.
ALTER TABLE `user` ADD COLUMN `point` decimal(38,2) NOT NULL DEFAULT 0;
ALTER TABLE `user` ADD COLUMN `total_purchase` decimal(38,2) NOT NULL DEFAULT 0;

-- 주문이 쓴/적립한 포인트. 취소 때 그대로 되돌리기 위해 주문에 기록해 둔다.
-- (적립률은 승급으로 바뀔 수 있어서 취소 시점에 다시 계산하면 금액이 어긋난다)
ALTER TABLE `orders` ADD COLUMN `used_point` decimal(38,2) NOT NULL DEFAULT 0;
ALTER TABLE `orders` ADD COLUMN `earned_point` decimal(38,2) NOT NULL DEFAULT 0;

-- 배송 상태 추가 (주문 완료 -> 배송 중 -> 배송 완료)
ALTER TABLE `orders` MODIFY COLUMN `status`
    enum('ORDERCANCEL','ORDERCOMPLETED','PENDING','SHIPPING','DELIVERED') NOT NULL;

-- 찜. 같은 상품을 두 번 찜하지 못하게 유니크로 막는다.
CREATE TABLE IF NOT EXISTS `wish` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wish_user_product` (`user_id`, `product_id`),
  CONSTRAINT `fk_wish_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `fk_wish_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
