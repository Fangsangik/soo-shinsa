package com.Soo_Shinsa.user.controller;

import com.Soo_Shinsa.user.service.KakaoAuthService;
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
public class KakaoUserController {

    private final KakaoAuthService kakaoAuthService;

    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> getToken(@RequestHeader("Authorization") String kakaoAccessToken) {
        return ResponseEntity.ok(kakaoAuthService.generateJwtFromKakaoToken(kakaoAccessToken.replace("Bearer ", "")));
    }

}
