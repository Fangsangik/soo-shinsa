package com.Soo_Shinsa.global.config;

import com.Soo_Shinsa.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP 계층 인가 규칙 회귀 테스트.
 *
 * 서비스 단위 테스트는 시큐리티 체인을 거치지 않으므로, 관리자 전용 경로가 열려 있거나
 * 결제 취소가 비로그인으로 호출되는 문제를 잡지 못한다. 실제로 그런 구멍이 있었다.
 * 여기서는 컨트롤러에 닿기 전 필터 체인이 막는지만 본다(요청 본문은 신경 쓰지 않는다).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRuleTest extends IntegrationTestSupport {

    @Autowired private MockMvc mvc;

    @Test
    void 브랜드_관리자_경로는_비로그인을_막는다() throws Exception {
        mvc.perform(get("/brands/admin/pending")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/brands/admin/1/approve")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void 브랜드_관리자_경로는_고객을_막는다() throws Exception {
        mvc.perform(get("/brands/admin/pending")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void 브랜드_승인은_사장도_못한다() throws Exception {
        mvc.perform(patch("/brands/admin/1/approve")).andExpect(status().isForbidden());
    }

    @Test
    void 결제_취소는_비로그인을_막는다() throws Exception {
        mvc.perform(post("/api/cancel")).andExpect(status().isUnauthorized());
    }

    @Test
    void 주문_조회는_비로그인을_막는다() throws Exception {
        mvc.perform(get("/orders/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void 상품_탐색은_비로그인도_열려_있다() throws Exception {
        mvc.perform(get("/products/search").param("page", "1").param("size", "10"))
                .andExpect(status().isOk());
        mvc.perform(get("/products/autocomplete").param("keyword", "셔츠"))
                .andExpect(status().isOk());
    }

    @Test
    void 운영_지표는_외부에_노출되지_않는다() throws Exception {
        // exposure.include 가 health,info 뿐이라 prometheus 는 매핑이 없고,
        // 화이트리스트에도 없으니 anyRequest().authenticated() 에서 먼저 걸린다
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
