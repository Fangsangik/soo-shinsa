package com.Soo_Shinsa.user.service;

import com.Soo_Shinsa.auth.dto.JwtAuthResponseDto;
import com.Soo_Shinsa.user.dto.*;
import com.Soo_Shinsa.user.model.User;


public interface UserService {
    UserResponseDto create(SignInRequestDto dto);

    JwtAuthResponseDto login(LoginRequestDto dto);


    UserDetailResponseDto getUser(User user);

    UserDetailResponseDto updateUser(User user, UserUpdateRequestDto userUpdateRequestDto);

    void leave(String password, User user);
}
