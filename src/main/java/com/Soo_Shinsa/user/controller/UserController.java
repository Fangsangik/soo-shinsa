package com.Soo_Shinsa.user.controller;

import com.Soo_Shinsa.auth.*;
import com.Soo_Shinsa.auth.dto.JwtAuthResponseDto;
import com.Soo_Shinsa.auth.dto.RefreshTokenRequestDto;
import com.Soo_Shinsa.user.dto.*;
import com.Soo_Shinsa.user.service.UserService;
import com.Soo_Shinsa.utils.CommonResponse;
import com.Soo_Shinsa.utils.ResponseMessage;
import com.Soo_Shinsa.utils.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {

    private final JwtRefreshTokenService jwtRefreshTokenService;
    private final JwtAccessTokenService jwtAccessTokenService;
    private final JwtBlackListService jwtBlackListService;
    private final JwtProvider jwtProvider;
    private final UserService userService;


    @PostMapping("/signin")
    public ResponseEntity<CommonResponse<UserResponseDto>> registerUser(@Valid @RequestBody SignInRequestDto dto) {
        UserResponseDto saved = userService.create(dto);
        CommonResponse<UserResponseDto> response = new CommonResponse<>(ResponseMessage.USER_SIGN_IN_SUCCESS, saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<JwtAuthResponseDto>> login(@RequestBody LoginRequestDto dto) {
        JwtAuthResponseDto login = userService.login(dto);

        jwtAccessTokenService.saveAccessToken(login.getAccessToken(), login.getEmail(), jwtProvider.getExpiryMillis());
        jwtRefreshTokenService.saveRefreshToken(login.getEmail(), login.getRefreshToken(), login.getRefreshTokenExpiration());

        return ResponseEntity.ok(new CommonResponse<>(ResponseMessage.USER_LOG_IN_SUCCESS, login));
    }


    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(HttpServletRequest request) {
        try {
            userService.logout(request);
            return ResponseEntity.ok(new CommonResponse<>(ResponseMessage.USER_LOG_OUT_SUCCESS, null));
        } catch (RuntimeException e) {
            log.warn("로그아웃 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonResponse<>(e.getMessage(), null));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<CommonResponse<JwtAuthResponseDto>> refreshAccessToken (@RequestBody RefreshTokenRequestDto requestDto) {
        JwtAuthResponseDto jwtAuthResponseDto = userService.refreshAccessToken(requestDto);
        return ResponseEntity.ok(new CommonResponse<>(ResponseMessage.USER_ACCESS_TOKEN_REFRESHED, jwtAuthResponseDto));
    }


    @GetMapping
    public ResponseEntity<CommonResponse<UserDetailResponseDto>> getUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserDetailResponseDto userDetailResponseDto = userService.getUser(UserUtils.getUser(userDetails));
        CommonResponse<UserDetailResponseDto> response = new CommonResponse<>(ResponseMessage.USER_SELECT_SUCCESS, userDetailResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping
    public ResponseEntity<CommonResponse<UserDetailResponseDto>> updateUser(@AuthenticationPrincipal UserDetails userDetails,
                                                                            @Valid @RequestBody UserUpdateRequestDto userUpdateRequestDto) {
        UserDetailResponseDto userDetailResponseDto = userService.updateUser(UserUtils.getUser(userDetails), userUpdateRequestDto);
        CommonResponse<UserDetailResponseDto> response = new CommonResponse<>(ResponseMessage.USER_SELECT_SUCCESS, userDetailResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leave(@RequestBody LeaveRequestDto dto,
                                      @AuthenticationPrincipal UserDetailsImp authenticatedPrincipal) {
        userService.leave(dto.getPassword(), authenticatedPrincipal.getUser());
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
