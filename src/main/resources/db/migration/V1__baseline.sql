-- 기존 스키마 baseline. ddl-auto=update 가 만들어 둔 스키마를 그대로 옮겼다.
-- 이미 이 스키마를 가진 DB 는 baseline-on-migrate 가 건너뛴다.

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `brand` (
  `coupon_count` int DEFAULT NULL,
  `is_coupon_limited` bit(1) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sub_category_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `context` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `registration_num` varchar(255) DEFAULT NULL,
  `status` enum('APPLY','OPEN','REJECT') DEFAULT NULL,
  `admin_comment` varchar(255) DEFAULT NULL,
  `approval_date` datetime(6) DEFAULT NULL,
  `approval_reason` varchar(255) DEFAULT NULL,
  `approved_by` bigint DEFAULT NULL,
  `rejection_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKf09gd6irroptyibpn19va3j1x` (`sub_category_id`),
  KEY `FK8gg6sb1nu1vha9lxgkb8lyeeq` (`user_id`),
  CONSTRAINT `FK8gg6sb1nu1vha9lxgkb8lyeeq` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `FKf09gd6irroptyibpn19va3j1x` FOREIGN KEY (`sub_category_id`) REFERENCES `sub_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `cart_item` (
  `discounted_price` decimal(38,2) DEFAULT NULL,
  `quantity` int NOT NULL,
  `coupon_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9uecacb9sxb290ytecwctnbe7` (`coupon_id`),
  KEY `FKjcyd5wv4igqnw413rgxbfu4nv` (`product_id`),
  KEY `FKjnaj4sjyqjkr4ivemf9gb25w` (`user_id`),
  CONSTRAINT `FK9uecacb9sxb290ytecwctnbe7` FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`id`),
  CONSTRAINT `FKjcyd5wv4igqnw413rgxbfu4nv` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`),
  CONSTRAINT `FKjnaj4sjyqjkr4ivemf9gb25w` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `cartitem_product_options` (
  `cartitem_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `productoption_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkv0e1482me2f6kdywt6ussa4o` (`cartitem_id`),
  KEY `FK7ywse9ekdsdnv1vub27fov9q2` (`productoption_id`),
  CONSTRAINT `FK7ywse9ekdsdnv1vub27fov9q2` FOREIGN KEY (`productoption_id`) REFERENCES `product_option` (`id`),
  CONSTRAINT `FKkv0e1482me2f6kdywt6ussa4o` FOREIGN KEY (`cartitem_id`) REFERENCES `cart_item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `category` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `coupon` (
  `discount_rate` decimal(38,2) DEFAULT NULL,
  `expiration_date` date DEFAULT NULL,
  `is_used` bit(1) NOT NULL,
  `issue_date` date DEFAULT NULL,
  `issued_count` int DEFAULT NULL,
  `max_count` int DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint DEFAULT NULL,
  `coupon_code` varchar(255) DEFAULT NULL,
  `coupon_name` varchar(255) DEFAULT NULL,
  `coupon_type` enum('SPECIFIC_BRAND','SPECIFIC_PRODUCT','UNIVERSAL') DEFAULT NULL,
  `remaining_count` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKla5fqtoqda90ovhtrqhw7v61p` (`product_id`),
  CONSTRAINT `FKla5fqtoqda90ovhtrqhw7v61p` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `coupon_brand_relation` (
  `brand_id` bigint NOT NULL,
  `coupon_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  KEY `FKfvpt6ex47n31w6sqn27r3gj5q` (`brand_id`),
  KEY `FKp2u9mt1qrflfu40j2dgwqd4m0` (`coupon_id`),
  CONSTRAINT `FKfvpt6ex47n31w6sqn27r3gj5q` FOREIGN KEY (`brand_id`) REFERENCES `brand` (`id`),
  CONSTRAINT `FKp2u9mt1qrflfu40j2dgwqd4m0` FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `coupon_user` (
  `is_used` bit(1) NOT NULL,
  `used_at` date DEFAULT NULL,
  `coupon_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKkkn2jxhpgkf8kce5ipsvak6vi` (`coupon_id`,`user_id`),
  KEY `FKbvhy4yneyu9jrfluk0ixqrp2r` (`user_id`),
  CONSTRAINT `FKbvhy4yneyu9jrfluk0ixqrp2r` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `FKeyxuurk92vehhs6mj2nq40oni` FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `grade` (
  `point_rate` decimal(38,2) NOT NULL,
  `requirement` decimal(38,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `grade_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `name` enum('BRONZE','GOLD','ROOKIE','SILVER') NOT NULL,
  PRIMARY KEY (`grade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `image` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `extension` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `origin_name` varchar(255) DEFAULT NULL,
  `path` varchar(255) DEFAULT NULL,
  `target_type` enum('BRAND','PRODUCT','REVIEW') DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `kakao_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `kakao_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `nickname` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnkyylowk4j5k6754iri7ui84a` (`user_id`),
  CONSTRAINT `FK7sipvamc38o09o8gp9v7kaius` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `order_items` (
  `discount_price` decimal(38,2) DEFAULT NULL,
  `price` decimal(38,2) DEFAULT NULL,
  `quantity` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orders_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `product_option_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `cancel_reason` varchar(255) DEFAULT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `status` enum('CANCELLED','ORDERED','REFUNDED') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqmxbj1e77sls50umaww7pkpnx` (`orders_id`),
  KEY `FKlf6f9q956mt144wiv6p1yko16` (`product_id`),
  KEY `FKi8f2ty6j99d94a35uoplc3nv9` (`product_option_id`),
  CONSTRAINT `FKi8f2ty6j99d94a35uoplc3nv9` FOREIGN KEY (`product_option_id`) REFERENCES `product_option` (`id`),
  CONSTRAINT `FKlf6f9q956mt144wiv6p1yko16` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`),
  CONSTRAINT `FKqmxbj1e77sls50umaww7pkpnx` FOREIGN KEY (`orders_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `orders` (
  `discount_price` decimal(38,2) DEFAULT NULL,
  `total_price` decimal(38,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `order_id` varchar(255) NOT NULL,
  `status` enum('ORDERCANCEL','ORDERCOMPLETED','PENDING') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKel9kyl84ego2otj2accfd8mr7` (`user_id`),
  CONSTRAINT `FKel9kyl84ego2otj2accfd8mr7` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `payments` (
  `amount` decimal(38,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orders_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `users_id` bigint NOT NULL,
  `order_id` varchar(255) NOT NULL,
  `payment_key` varchar(255) DEFAULT NULL,
  `method` enum('CARD') NOT NULL,
  `status` enum('CANCEL','PAYMENT','PENDING','REFUND') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKfkgfkd9am8ya47gt154tc2o89` (`orders_id`),
  UNIQUE KEY `UK35yqdahtiysne6iij9ske72bj` (`payment_key`),
  KEY `FKatjnt6uxa78ba96khn9ta7amf` (`users_id`),
  CONSTRAINT `FK3a4hdsi0cvuqqogqutius6wg0` FOREIGN KEY (`orders_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `FKatjnt6uxa78ba96khn9ta7amf` FOREIGN KEY (`users_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `product` (
  `price` decimal(15,2) NOT NULL,
  `brand_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `product_status` enum('AVAILABLE','DISCONTINUED','SOLD_OUT','UNAVAILABLE') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKs6cydsualtsrprvlf2bb3lcam` (`brand_id`),
  FULLTEXT KEY `ft_product_name` (`name`) WITH PARSER ngram,
  CONSTRAINT `FKs6cydsualtsrprvlf2bb3lcam` FOREIGN KEY (`brand_id`) REFERENCES `brand` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `product_option` (
  `quantity` int DEFAULT NULL,
  `sales_count` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `color` varchar(255) DEFAULT NULL,
  `size` varchar(255) DEFAULT NULL,
  `product_status` enum('AVAILABLE','DISCONTINUED','SOLD_OUT','UNAVAILABLE') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKn4hmm6ex1vgn60c6uiqte400f` (`product_id`),
  CONSTRAINT `FKn4hmm6ex1vgn60c6uiqte400f` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `report` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `content` varchar(255) NOT NULL,
  `reject_reason` varchar(255) DEFAULT NULL,
  `status` enum('APPROVED','IN_PROGRESS','OPEN','REJECTED','RESOLVED') NOT NULL,
  `target_type` enum('BRAND','PRODUCT','REVIEW') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKj62onw73yx1qnmd57tcaa9q3a` (`user_id`),
  CONSTRAINT `FKj62onw73yx1qnmd57tcaa9q3a` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `review` (
  `rate` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_item_id` bigint DEFAULT NULL,
  `product_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `content` varchar(255) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKayarlhrlpo6aar4prnvdy56j8` (`order_item_id`),
  KEY `FKiyof1sindb9qiqr9o8npj8klt` (`product_id`),
  KEY `FKiyf57dy48lyiftdrf7y87rnxi` (`user_id`),
  CONSTRAINT `FKayarlhrlpo6aar4prnvdy56j8` FOREIGN KEY (`order_item_id`) REFERENCES `order_items` (`id`),
  CONSTRAINT `FKiyf57dy48lyiftdrf7y87rnxi` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `FKiyof1sindb9qiqr9o8npj8klt` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `statistics` (
  `order_date` date DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `total_price` decimal(38,2) DEFAULT NULL,
  `statistics_id` bigint NOT NULL,
  `brand_name` varchar(255) DEFAULT NULL,
  `category_name` varchar(255) DEFAULT NULL,
  `order_status` varchar(255) DEFAULT NULL,
  `product_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`statistics_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `statistics_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `sub_category` (
  `category_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKl65dyy5me2ypoyj8ou1hnt64e` (`category_id`),
  CONSTRAINT `FKl65dyy5me2ypoyj8ou1hnt64e` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `user` (
  `user_grade_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `phone_num` varchar(255) NOT NULL,
  `role` enum('ADMIN','CUSTOMER','VENDOR') NOT NULL,
  `status` enum('ACTIVE','DELETED','UN_ACTIVE') NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UKob8kqyqqgmefl0aco34akdtpe` (`email`),
  KEY `FK4duco0vi4bruki4ngw5x5w2de` (`user_grade_id`),
  CONSTRAINT `FK4duco0vi4bruki4ngw5x5w2de` FOREIGN KEY (`user_grade_id`) REFERENCES `user_grade` (`user_grade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `user_grade` (
  `created_at` datetime(6) DEFAULT NULL,
  `grade_id` bigint DEFAULT NULL,
  `user_grade_id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`user_grade_id`),
  KEY `FK27wm2yaakhdy34rffwqq4r4h3` (`grade_id`),
  CONSTRAINT `FK27wm2yaakhdy34rffwqq4r4h3` FOREIGN KEY (`grade_id`) REFERENCES `grade` (`grade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `user_product_view` (
  `view_date` date DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_option_id` bigint DEFAULT NULL,
  `user_user_id` bigint DEFAULT NULL,
  `product_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjhba3cyeg01ifndqw07c2ief4` (`product_option_id`),
  KEY `FKsn0i86w2fwv660muv3fb5qfm3` (`user_user_id`),
  KEY `FK4f0ctdvvcsbft2x0e02kf7jjc` (`product_id`),
  CONSTRAINT `FK4f0ctdvvcsbft2x0e02kf7jjc` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`),
  CONSTRAINT `FKjhba3cyeg01ifndqw07c2ief4` FOREIGN KEY (`product_option_id`) REFERENCES `product_option` (`id`),
  CONSTRAINT `FKsn0i86w2fwv660muv3fb5qfm3` FOREIGN KEY (`user_user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
