package com.Soo_Shinsa.auth;

import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import com.Soo_Shinsa.utils.UserUtils;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static com.Soo_Shinsa.support.Fixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 슬라이딩 세션: 매 요청 DB 조회 없이 Redis 세션 키로 인증하고, 키가 없으면(유휴 30분) 블랙리스트 + 강제 로그아웃. */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {
    @Mock UserRepository userRepository;
    @Mock JwtBlackListService blackList;
    @Mock JwtAccessTokenService accessTokens;
    @Mock JwtRefreshTokenService refreshTokens;
    @Mock FilterChain chain;

    JwtProvider provider;
    JwtAuthFilter filter;
    String token;

    @BeforeEach
    void setUp() {
        provider = new JwtProvider(userRepository, blackList);
        ReflectionTestUtils.setField(provider, "secret", "unit-test-secret-key-that-is-at-least-32-bytes-long!!");
        ReflectionTestUtils.setField(provider, "expiryMillis", 3_600_000L);
        User dbUser = user(42L, Role.VENDOR);
        when(userRepository.findByEmailOrElseThrow(dbUser.getEmail())).thenReturn(dbUser);
        token = provider.generateTokenBy(dbUser.getEmail(), 3_600_000L);
        reset(userRepository); // 이후로는 DB 를 건드리면 안 된다

        filter = new JwtAuthFilter(provider, blackList, accessTokens, refreshTokens);
        ReflectionTestUtils.setField(filter, "idleMillis", 1_800_000L);
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private MockHttpServletRequest req(String uri) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", uri);
        r.setRequestURI(uri);
        r.addHeader("Authorization", "Bearer " + token);
        return r;
    }

    @Test
    void 세션이_살아있으면_DB없이_클레임으로_인증하고_유휴시간을_연장한다() throws Exception {
        when(accessTokens.getAccessToken(token)).thenReturn("vendor42@test.com");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req("/orders/manage"), res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        User principal = UserUtils.getUser((UserDetails) auth.getPrincipal());
        assertThat(principal.getUserId()).isEqualTo(42L);
        assertThat(principal.getRole()).isEqualTo(Role.VENDOR);
        assertThat(principal.getEmail()).isEqualTo("vendor42@test.com");
        assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_VENDOR");
        verify(accessTokens).touch(token, 1_800_000L);
        verify(chain).doFilter(any(), any());
        verifyNoInteractions(userRepository, blackList, refreshTokens);
    }

    @Test
    void 유휴_만료된_세션은_401과_함께_블랙리스트_및_refresh_삭제로_강제_로그아웃() throws Exception {
        when(accessTokens.getAccessToken(token)).thenReturn(null);
        when(blackList.isBlackListed(token)).thenReturn(false);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req("/orders/users"), res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentAsString()).contains("\"code\":\"SESSION_EXPIRED\"").contains("30분");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(blackList).addBlackList(eq(token), longThat(ms -> ms > 0 && ms <= 3_600_000L));
        verify(refreshTokens).deleteRefreshToken("vendor42@test.com");
        verify(accessTokens, never()).touch(any(), anyLong());
        verifyNoInteractions(chain, userRepository);
    }

    @Test
    void uid_클레임이_없는_구버전_토큰은_INVALID_TOKEN으로_재로그인_유도() throws Exception {
        String legacy = io.jsonwebtoken.Jwts.builder().subject("vendor42@test.com").claim("role", "VENDOR")
                .expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("unit-test-secret-key-that-is-at-least-32-bytes-long!!".getBytes())).compact();
        MockHttpServletRequest r = new MockHttpServletRequest("GET", "/users"); r.setRequestURI("/users"); r.addHeader("Authorization", "Bearer " + legacy);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(r, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentAsString()).contains("INVALID_TOKEN");
        verifyNoInteractions(chain, accessTokens);
    }

    @Test
    void 토큰이_없으면_통과시키고_위조되면_INVALID_TOKEN_401() throws Exception {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", "/brands"); r.setRequestURI("/brands");
        filter.doFilter(r, new MockHttpServletResponse(), chain);
        MockHttpServletRequest bad = req("/brands"); bad.removeHeader("Authorization"); bad.addHeader("Authorization", "Bearer " + token + "x");
        MockHttpServletResponse badRes = new MockHttpServletResponse();
        filter.doFilter(bad, badRes, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, times(1)).doFilter(any(), any());
        assertThat(badRes.getStatus()).isEqualTo(401);
        assertThat(badRes.getContentAsString()).contains("INVALID_TOKEN");
        verifyNoInteractions(accessTokens, refreshTokens, blackList);
    }
}
