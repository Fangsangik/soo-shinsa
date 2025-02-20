package com.Soo_Shinsa.user.dto;

import com.Soo_Shinsa.user.model.KakaoUser;
import com.Soo_Shinsa.user.model.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoUserCreateRequestDto {
    private Long kakaoId;
    private String email;
    private String nickname;
    private User user;

    @Builder
    public KakaoUserCreateRequestDto(Long kakaoId, String email, String nickname, User user) {
        this.kakaoId = kakaoId;
        this.email = email;
        this.nickname = nickname;
        this.user = user;
    }

    public KakaoUser toEntity() {
        return KakaoUser.builder()
                .kakaoId(kakaoId)
                .email(email)
                .nickname(nickname)
                .user(user)
                .build();
    }
}
