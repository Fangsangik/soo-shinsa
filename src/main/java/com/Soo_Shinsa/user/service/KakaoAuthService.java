package com.Soo_Shinsa.user.service;

import com.Soo_Shinsa.auth.JwtProvider;
import com.Soo_Shinsa.user.dto.KakaoUserInfoResponseDto;
import com.Soo_Shinsa.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final KakaoService kakaoService;
    private final UserService userService;
    private final JwtProvider jwtProvider;

    @Transactional
    public Map<String, String> kakaoLogin(String kakaoAccessToken) {
        // 1️⃣ 카카오에서 유저 정보 가져오기
        KakaoUserInfoResponseDto kakaoUserInfo = kakaoService.getUserInfo(kakaoAccessToken.replace("Bearer ", ""));

        // 2️⃣ 기존 유저 조회 또는 새 유저 생성
        UserResponseDto user = userService.findOrCreateKakaoUser(kakaoUserInfo);

        // 3️⃣ JWT 생성 (기존 메서드 활용)
        String accessToken = jwtProvider.generateTokenBy(user.getEmail(), jwtProvider.getExpiryMillis());
        String refreshToken = jwtProvider.generateTokenBy(user.getEmail(), jwtProvider.getRefreshExpiryMillis());

        // 4️⃣ JWT 반환
        Map<String, String> jwtTokens = new HashMap<>();
        jwtTokens.put("accessToken", accessToken);
        jwtTokens.put("refreshToken", refreshToken);
        return jwtTokens;
    }

    @Transactional
    public void logoutKakapUser(String accessToken) {
        // 카카오 로그아웃
        WebClient.create()
                .post()
                .uri("https://kapi.kakao.com/v1/user/logout")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
