package com.Soo_Shinsa.user.dto;

import com.Soo_Shinsa.global.constant.Role;
import com.Soo_Shinsa.global.constant.UserStatus;
import com.Soo_Shinsa.user.model.User;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserCreateRequestDto {
    private String email;
    private String name;
    private String password;
    private Role role;
    private String phoneNum;

    @Builder
    public UserCreateRequestDto(String email, String name, String password, Role role, String phoneNum) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.role = role;
        this.phoneNum = phoneNum;
    }

    public User toEntity() {
        return User.builder()
                .email(email)
                .name(name)
                .password(password)
                .role(role)
                .phoneNum(phoneNum)  // 기본값
                .status(UserStatus.ACTIVE)  // 기본값
                .build();
    }
}