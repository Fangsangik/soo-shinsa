package com.Soo_Shinsa.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoUserInfoResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KakaoAccount {

        @JsonProperty("email")
        private String email;

        @JsonProperty("profile")
        private Profile profile;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Profile {
        @JsonProperty("nickname")
        private String nickname;
    }

    // ✅ Map<String, Object>를 이용한 객체 생성 메서드
    public static KakaoUserInfoResponseDto fromAttributes(Map<String, Object> attributes) {
        Long id = (Long) attributes.get("id");

        Map<String, Object> kakaoAccountMap = (Map<String, Object>) attributes.get("kakao_account");
        KakaoAccount kakaoAccount = null;

        if (kakaoAccountMap != null) {
            String email = (String) kakaoAccountMap.get("email");

            Map<String, Object> profileMap = (Map<String, Object>) kakaoAccountMap.get("profile");
            Profile profile = (profileMap != null) ? Profile.builder()
                    .nickname((String) profileMap.get("nickname"))
                    .build() : null;

            kakaoAccount = KakaoAccount.builder()
                    .email(email)
                    .profile(profile)
                    .build();
        }

        return KakaoUserInfoResponseDto.builder()
                .id(id)
                .kakaoAccount(kakaoAccount)
                .build();
    }
}
