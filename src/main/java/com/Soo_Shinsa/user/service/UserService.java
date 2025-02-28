package com.Soo_Shinsa.user.service;

import com.Soo_Shinsa.global.auth.dto.JwtAuthResponseDto;
import com.Soo_Shinsa.global.auth.dto.RefreshTokenRequestDto;
import com.Soo_Shinsa.user.dto.*;
import com.Soo_Shinsa.user.model.User;
import jakarta.servlet.http.HttpServletRequest;


public interface UserService {
    UserResponseDto create(SignInRequestDto dto);

    UserResponseDto findOrCreateKakaoUser(KakaoUserInfoResponseDto kakaoUserInfo);

    JwtAuthResponseDto login(LoginRequestDto dto);

    void logout(HttpServletRequest request);

    UserDetailResponseDto getUser(User user);

    UserDetailResponseDto updateUser(User user, UserUpdateRequestDto userUpdateRequestDto);

    void leave(String password, User user);

    JwtAuthResponseDto refreshAccessToken(RefreshTokenRequestDto requestDto);
}
