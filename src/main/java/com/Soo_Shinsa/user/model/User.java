package com.Soo_Shinsa.user.model;

import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.constant.UserStatus;
import com.Soo_Shinsa.user.dto.UserUpdateRequestDto;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phoneNum;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_grade_id")
    private UserGrade userGrade;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private KakaoUser kakaoUser;

    @Builder
    public User(String email, String password, String name, String phoneNum, UserStatus status, Role role, UserGrade userGrade, KakaoUser kakaoUser) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNum = phoneNum;
        this.status = status;
        this.role = role;
        this.userGrade = userGrade;
        this.kakaoUser = kakaoUser;
    }

    public void updateUserGrade(UserGrade userGrade) {
        this.userGrade = userGrade;
    }

    public void delete() {
        this.status = UserStatus.DELETED;
    }

    public void update(UserUpdateRequestDto userUpdateRequestDto) {
        this.name = userUpdateRequestDto.getName();
        this.phoneNum = userUpdateRequestDto.getPhoneNum();
    }
    public void updatePassword(String password) {
        this.password = password;
    }

    // 역할 검증 메소드
    public boolean isAdmin() {
        return Role.ADMIN.equals(this.role);
    }

    public boolean isVendor() {
        return Role.VENDOR.equals(this.role);
    }

    public boolean isAdminOrVendor() {
        return isAdmin() || isVendor();
    }

    public void assignKakaoUser(KakaoUser kakaoUser) {
        this.kakaoUser = kakaoUser;
        kakaoUser.assignUser(this);
    }
}