package com.Soo_Shinsa.user.service;

import com.Soo_Shinsa.global.auth.JwtProvider;
import com.Soo_Shinsa.user.dto.KakaoUserInfoResponseDto;
import com.Soo_Shinsa.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final JwtProvider jwtProvider;
    private final UserService userService;
    private final WebClient webClient = WebClient.builder().baseUrl("https://kauth.kakao.com").build();

    public Map<String, String> generateJwtFromKakaoToken(String kakaoAccessToken) {
        // ✅ 1️⃣ Kakao API 호출해서 사용자 정보 가져오기
        KakaoUserInfoResponseDto userInfo = getUserInfoFromKakao(kakaoAccessToken);

        if (userInfo.getKakaoAccount() == null || userInfo.getKakaoAccount().getEmail() == null) {
            throw new IllegalArgumentException("카카오 계정에 이메일이 없습니다.");
        }

        // ✅ 2️⃣ 기존 사용자 조회 또는 신규 생성
        UserResponseDto user = userService.findOrCreateKakaoUser(userInfo);

        // ✅ 3️⃣ JWT 생성
        String accessToken = jwtProvider.generateTokenBy(user.getEmail(), jwtProvider.getExpiryMillis());
        String refreshToken = jwtProvider.generateTokenBy(user.getEmail(), jwtProvider.getRefreshExpiryMillis());

        // ✅ 4️⃣ JWT 반환
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        return tokens;
    }

    private KakaoUserInfoResponseDto getUserInfoFromKakao(String kakaoAccessToken) {
        // ✅ Kakao API 요청 보내기
        Map<String, Object> response = webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .headers(headers -> headers.setBearerAuth(kakaoAccessToken))
                .retrieve()
                .bodyToMono(Map.class)
                .block(); // 동기 요청

        // ✅ Kakao 응답 데이터를 KakaoUserInfoResponseDto로 변환
        return KakaoUserInfoResponseDto.fromAttributes(response);
    }


}
