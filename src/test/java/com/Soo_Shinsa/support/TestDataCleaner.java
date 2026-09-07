package com.Soo_Shinsa.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * 테스트 픽스처 정리.
 *
 * 기존 테스트들은 test@test.com 같은 고정 이메일로 유저/브랜드/상품/쿠폰을 만들면서
 * 지우지는 않아서, 두 번째 실행부터 Duplicate entry 와 FK 위반으로 깨졌다.
 * 각 테스트의 setUp/tearDown 에서 이걸 호출해 자기 픽스처만 지운다.
 * 시드 데이터나 직접 만든 계정은 이메일 패턴이 달라 건드리지 않는다.
 */
public final class TestDataCleaner {

    /** 테스트가 쓰는 이메일: test@test.com, test0@test.com ... test999@test.com */
    private static final String TEST_USERS =
            "(SELECT user_id FROM (SELECT user_id FROM user " +
            "WHERE email = 'test@test.com' OR email REGEXP '^test[0-9]+@test\\\\.com$') u)";

    private static final String TEST_BRANDS =
            "(SELECT id FROM (SELECT id FROM brand WHERE user_id IN " + TEST_USERS + ") b)";

    private static final String TEST_PRODUCTS =
            "(SELECT id FROM (SELECT id FROM product WHERE brand_id IN " + TEST_BRANDS + ") p)";

    private static final String TEST_ORDERS =
            "(SELECT id FROM (SELECT id FROM orders WHERE user_id IN " + TEST_USERS + ") o)";

    private static final String TEST_CARTS =
            "(SELECT id FROM (SELECT id FROM cart_item WHERE user_id IN " + TEST_USERS +
            " OR product_id IN " + TEST_PRODUCTS + ") c)";

    /** 테스트가 만든 쿠폰: 테스트 브랜드에 걸렸거나 테스트 유저에게 발급된 것 */
    private static final String SELECT_TEST_COUPON_IDS =
            "SELECT coupon_id FROM coupon_brand_relation WHERE brand_id IN " + TEST_BRANDS +
            " UNION SELECT coupon_id FROM coupon_user WHERE user_id IN " + TEST_USERS +
            " UNION SELECT id FROM coupon WHERE product_id IN " + TEST_PRODUCTS;

    /** FK 자식 -> 부모 순서. 순서를 바꾸면 제약조건에 걸린다. */
    private static final String[] STATEMENTS = {
            "DELETE FROM cartitem_product_options WHERE cartitem_id IN " + TEST_CARTS,
            "DELETE FROM review WHERE user_id IN " + TEST_USERS + " OR product_id IN " + TEST_PRODUCTS,
            "DELETE FROM order_items WHERE orders_id IN " + TEST_ORDERS + " OR product_id IN " + TEST_PRODUCTS,
            "DELETE FROM payments WHERE users_id IN " + TEST_USERS + " OR orders_id IN " + TEST_ORDERS,
            "DELETE FROM orders WHERE user_id IN " + TEST_USERS,
            "DELETE FROM cart_item WHERE user_id IN " + TEST_USERS + " OR product_id IN " + TEST_PRODUCTS,
            "DELETE FROM coupon_user WHERE user_id IN " + TEST_USERS,
            "DELETE FROM user_product_view WHERE user_user_id IN " + TEST_USERS,
            "DELETE FROM report WHERE user_id IN " + TEST_USERS,
            "DELETE FROM kakao_user WHERE user_id IN " + TEST_USERS,
            "DELETE FROM product_option WHERE product_id IN " + TEST_PRODUCTS,
            "DELETE FROM coupon_brand_relation WHERE brand_id IN " + TEST_BRANDS,
            "DELETE FROM product WHERE brand_id IN " + TEST_BRANDS,
            "DELETE FROM brand WHERE user_id IN " + TEST_USERS,
            "DELETE FROM user WHERE user_id IN " + TEST_USERS,
    };

    private TestDataCleaner() {
    }

    /**
     * 아무 데도 연결되지 않은 쿠폰.
     * 테스트는 만료 쿠폰처럼 브랜드도 발급도 없는 쿠폰을 만들어 두고 지우지 않아서 계속 쌓인다.
     *
     * 주의: 브랜드도 발급 이력도 없는 쿠폰을 직접 만들어 두었다면 이 정리에 함께 지워진다.
     * 테스트 전용 헬퍼이고 대상은 개발 DB 라는 전제다.
     */
    private static final String SELECT_ORPHAN_COUPON_IDS =
            "SELECT id FROM coupon WHERE id NOT IN (SELECT coupon_id FROM coupon_brand_relation) " +
            "AND id NOT IN (SELECT coupon_id FROM coupon_user) " +
            "AND id NOT IN (SELECT coupon_id FROM cart_item WHERE coupon_id IS NOT NULL)";

    public static void clean(JdbcTemplate jdbc) {
        // 쿠폰 id 는 관계/발급 이력으로 찾으므로, 그것들을 지우기 전에 먼저 확보해야 한다.
        // 순서를 바꾸면 목록이 비어버려 쿠폰이 계속 쌓인다.
        deleteCoupons(jdbc, jdbc.queryForList(SELECT_TEST_COUPON_IDS, Long.class));

        for (String sql : STATEMENTS) {
            jdbc.update(sql);
        }

        // 위 정리로 연결이 끊긴 쿠폰까지 마지막에 걷어낸다
        deleteCoupons(jdbc, jdbc.queryForList(SELECT_ORPHAN_COUPON_IDS, Long.class));
    }

    private static void deleteCoupons(JdbcTemplate jdbc, List<Long> couponIds) {
        if (couponIds.isEmpty()) {
            return;
        }
        String in = "(" + String.join(",", couponIds.stream().map(String::valueOf).toList()) + ")";
        jdbc.update("UPDATE cart_item SET coupon_id = NULL WHERE coupon_id IN " + in);
        jdbc.update("DELETE FROM coupon_user WHERE coupon_id IN " + in);
        jdbc.update("DELETE FROM coupon_brand_relation WHERE coupon_id IN " + in);
        jdbc.update("DELETE FROM coupon WHERE id IN " + in);
    }
}
