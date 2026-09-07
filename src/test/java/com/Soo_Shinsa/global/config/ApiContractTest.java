package com.Soo_Shinsa.global.config;

import com.Soo_Shinsa.support.IntegrationTestSupport;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static com.Soo_Shinsa.global.constant.UrlConst.API;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 조회 엔드포인트가 브라우저에서 부를 수 있는 모양인지 확인한다.
 *
 * 이것들이 @RequestBody 를 요구하고 있었다. GET 에는 본문을 실을 수 없으므로(fetch 가 막는다)
 * 프런트에서 호출 자체가 불가능했고, 본문 없이 부르면 400 "Required request body is missing" 이었다.
 *
 * 로그인 상태로 불러야 의미가 있다. 비로그인이면 시큐리티가 401 로 먼저 끊어서
 * 본문 요구 여부까지 가지도 못한다(그래서 어떤 코드든 통과해 버린다).
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class ApiContractTest extends IntegrationTestSupport {

    @Autowired private MockMvc mvc;

    private void 본문없이_부를_수_있다(String path) throws Exception {
        try {
            int status = mvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertNotEquals(400, status,
                    path + " 가 요청 본문을 요구한다. GET 은 본문을 실을 수 없으므로 프런트에서 못 부른다");
        } catch (ServletException e) {
            // 컨트롤러 본문까지 도달했다는 뜻(@WithMockUser 의 principal 캐스팅 실패).
            // 인자 해석 단계를 통과했으므로 이 테스트가 보려는 조건은 만족한다.
        }
    }

    @Test
    void 조회_엔드포인트는_요청_본문을_요구하지_않는다() throws Exception {
        본문없이_부를_수_있다(API + "/orders/users");
        본문없이_부를_수_있다(API + "/orders/users/summary");
        본문없이_부를_수_있다(API + "/orders/users/optimized");
        본문없이_부를_수_있다(API + "/order-items");
        본문없이_부를_수_있다(API + "/carts/users");
        본문없이_부를_수_있다(API + "/options?optionSize=M"); // 색상/사이즈 중 하나가 필수. size 는 페이징이라 optionSize 를 쓴다
        본문없이_부를_수_있다(API + "/admin/statistics/sales?periodType=DAY");
        본문없이_부를_수_있다(API + "/admin/statistics/count?periodType=DAY");
    }
}
