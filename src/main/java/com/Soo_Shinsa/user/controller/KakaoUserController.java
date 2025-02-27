package com.Soo_Shinsa.user.controller;

import com.Soo_Shinsa.user.service.KakaoAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "카카오 API", description = "카카오 로그인 기능을 제공")
public class KakaoUserController {

    private final KakaoAuthService kakaoAuthService;

    @Operation(summary = "카카오 로그인 사용자 토큰 생성", description = "카카오 로그인 사용자의 토큰을 애플리케이션 토큰으로 생성합니다.")
    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> getToken(@RequestHeader("Authorization") String kakaoAccessToken) {
        return ResponseEntity.ok(kakaoAuthService.generateJwtFromKakaoToken(kakaoAccessToken.replace("Bearer ", "")));
    }

}
