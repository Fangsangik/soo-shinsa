package com.Soo_Shinsa.user.controller;

import com.Soo_Shinsa.user.dto.KakaoTokenResponseDto;
import com.Soo_Shinsa.user.service.KakaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/kakao")
@RequiredArgsConstructor
@Tag(name = "Kakao Auth", description = "카카오 로그인 API")
public class KakaoUserController {

    private final KakaoService kakaoService;

    @Operation(summary = "카카오 액세스 토큰 요청",
            description = "카카오 OAuth2를 이용해 액세스 토큰을 요청합니다.")
    @PostMapping(value = "/token")
    public Mono<KakaoTokenResponseDto> getAccessToken(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("code") String code) {

        log.info("Swagger에서 받은 요청 데이터: grant_type={}, client_id={}, redirect_uri={}, code={}",
                grantType, clientId, redirectUri, code);

        return kakaoService.requestKakaoToken(grantType, clientId, redirectUri, code);
    }

    @PostMapping("/login")
    @Operation(summary = "카카오 로그인", description = "카카오 OAuth2를 이용하여 로그인을 진행합니다.",
            security = @SecurityRequirement(name = "KakaoOAuth2"))
    public ResponseEntity<Map<String, String>> login(@RequestHeader("Authorization") String kakaoAccessToken) {
        var userInfo = kakaoService.getUserInfo(kakaoAccessToken.replace("Bearer ", ""));
        Map<String, String> jwtTokens = kakaoService.createJwtFromKakaoUser(userInfo);
        return ResponseEntity.ok(jwtTokens);
    }

    @PostMapping("/logout")
    @Operation(summary = "카카오 로그아웃", description = "카카오 계정 로그아웃을 처리합니다.",
            security = @SecurityRequirement(name = "KakaoOAuth2"))
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String kakaoAccessToken) {
        kakaoService.logoutKakapUser(kakaoAccessToken.replace("Bearer ", ""));
        return ResponseEntity.noContent().build();
    }
}
