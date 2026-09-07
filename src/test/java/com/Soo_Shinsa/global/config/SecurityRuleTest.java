package com.Soo_Shinsa.global.config;

import com.Soo_Shinsa.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import jakarta.servlet.ServletException;
import org.springframework.test.web.servlet.MockMvc;

import static com.Soo_Shinsa.global.constant.UrlConst.API;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
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
        mvc.perform(get(API + "/brands/admin/pending")).andExpect(status().isUnauthorized());
        mvc.perform(patch(API + "/brands/admin/1/approve")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void 브랜드_관리자_경로는_고객을_막는다() throws Exception {
        mvc.perform(get(API + "/brands/admin/pending")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void 브랜드_승인은_사장도_못한다() throws Exception {
        mvc.perform(patch(API + "/brands/admin/1/approve")).andExpect(status().isForbidden());
    }

    @Test
    void 결제_API_는_비로그인을_막는다() throws Exception {
        mvc.perform(post(API + "/payments/cancel")).andExpect(status().isUnauthorized());
        mvc.perform(get(API + "/payments/checkout/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void 결제사_콜백은_로그인_없이_들어와_결과_페이지로_보낸다() throws Exception {
        // 토스가 브라우저를 돌려보내는 주소라 인증이 없다. 예전에는 templates 가 없어
        // 뷰 이름을 못 찾고 500 으로 끝났다.
        mvc.perform(get("/api/fail").param("orderId", "order-1").param("message", "사용자 취소"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/?payment=fail*"));
    }

    @Test
    void 주문_조회는_비로그인을_막는다() throws Exception {
        mvc.perform(get(API + "/orders/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void 내_정보는_역할과_무관하게_로그인만_하면_된다() throws Exception {
        // /users/** 를 hasRole("CUSTOMER") 로 묶어 둬서 업주/관리자가 403 이었다
        mvc.perform(get(API + "/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void 업주는_내_정보_경로에서_인가에_막히지_않는다() {
        // 보려는 것은 "필터 체인이 403 으로 끊지 않는다"는 것 하나다.
        // @WithMockUser 가 넣는 principal 은 UserDetailsImp 가 아니라서 컨트롤러 안에서
        // 캐스팅에 실패하는데, 그 예외가 났다는 것 자체가 컨트롤러까지 도달했다는 뜻이다.
        // /users/** 를 다시 hasRole("CUSTOMER") 로 묶으면 예외 없이 403 이 떨어져 이 테스트가 깨진다.
        ServletException e = assertThrows(ServletException.class,
                () -> mvc.perform(get(API + "/users")));
        assertInstanceOf(ClassCastException.class, e.getCause(),
                "인가가 아니라 컨트롤러에서 난 예외여야 한다");
    }

    @Test
    void 상품_탐색은_비로그인도_열려_있다() throws Exception {
        mvc.perform(get(API + "/products/search").param("page", "1").param("size", "10"))
                .andExpect(status().isOk());
        mvc.perform(get(API + "/products/autocomplete").param("keyword", "셔츠"))
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
