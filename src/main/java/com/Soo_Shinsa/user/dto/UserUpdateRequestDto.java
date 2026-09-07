package com.Soo_Shinsa.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class UserUpdateRequestDto {
    private String name;
    private String phoneNum;
    // 비밀번호는 바꿀 때만 채운다. 필수로 두는 바람에 이름/전화만 고치는 것이 불가능했다.
    private String oldPassword;
    private String newPassword;

    public UserUpdateRequestDto(String name, String phoneNum, String oldPassword, String newPassword) {
        this.name = name;
        this.phoneNum = phoneNum;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}

