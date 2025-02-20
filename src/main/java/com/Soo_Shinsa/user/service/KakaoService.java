package com.Soo_Shinsa.user.service;

import com.Soo_Shinsa.user.dto.KakaoTokenResponseDto;
import com.Soo_Shinsa.user.dto.KakaoUserInfoResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {

    @Value("${kakao.user_info_uri}")
    private String userInfoUri;

    private final WebClient webClient = WebClient.builder().baseUrl("https://kauth.kakao.com").build();

    public Mono<KakaoTokenResponseDto> requestKakaoToken(String grantType, String clientId, String redirectUri, String code) {
        // `&state=` 이후 값 제거
        String cleanedCode = cleanCode(code);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", grantType);
        formData.add("client_id", clientId);
        formData.add("redirect_uri", redirectUri);
        formData.add("code", cleanedCode); // 정제된 코드 사용

        log.info("📡 카카오 토큰 요청 데이터 (정제됨): {}", formData);

        return webClient.post()
                .uri("/oauth/token")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header(HttpHeaders.ACCEPT, "*/*")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(KakaoTokenResponseDto.class)
                .doOnSuccess(response -> log.info("카카오 토큰 요청 성공: {}", response))
                .doOnError(error -> log.error("WebClient 요청 중 에러 발생", error));
    }

    public KakaoUserInfoResponseDto getUserInfo(String accessToken) {
        KakaoUserInfoResponseDto response = WebClient.create()
                .get()
                .uri(userInfoUri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(KakaoUserInfoResponseDto.class)
                .block();

        return response;
    }

    private String cleanCode(String code) {
        return code.split("&state=")[0];  // "&state=" 이후 값 제거
    }
}
