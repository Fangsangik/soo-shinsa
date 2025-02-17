package com.Soo_Shinsa.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class UserUpdateRequestDto {
    private String name;
    private String phoneNum;
    @NotBlank(message = "기존 비밀번호를 입력해주세요.")
    private String oldPassword;
    @NotBlank(message = "새로운 비밀번호를 입력해주세요.")
    private String newPassword;

    public UserUpdateRequestDto(String name, String phoneNum, String oldPassword, String newPassword) {
        this.name = name;
        this.phoneNum = phoneNum;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}

