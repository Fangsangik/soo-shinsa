package com.Soo_Shinsa.user.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class KakaoUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String nickname;

    private Long kakaoId;

    @OneToOne(fetch = FetchType.LAZY)// 관계의 종속 엔티티
    @JoinColumn(name = "user_id") // User 테이블의 FK 설정
    private User user;

    @Builder
    public KakaoUser(String email, String nickname, Long kakaoId, User user) {
        this.email = email;
        this.nickname = nickname;
        this.kakaoId = kakaoId;
        this.user = user;
    }

    public void assignUser(User user) {
        this.user = user;

        if (user.getKakaoUser() != this) {
            user.assignKakaoUser(this);
        }
    }
}
