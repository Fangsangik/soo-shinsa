package com.Soo_Shinsa.user.controller;

import com.Soo_Shinsa.auth.JwtAccessTokenService;
import com.Soo_Shinsa.auth.JwtProvider;
import com.Soo_Shinsa.auth.JwtRefreshTokenService;
import com.Soo_Shinsa.auth.UserDetailsImp;
import com.Soo_Shinsa.auth.dto.JwtAuthResponseDto;
import com.Soo_Shinsa.auth.dto.RefreshTokenRequestDto;
import com.Soo_Shinsa.user.dto.*;
import com.Soo_Shinsa.user.service.UserService;
import com.Soo_Shinsa.utils.CommonResponse;
import com.Soo_Shinsa.utils.ResponseMessage;
import com.Soo_Shinsa.utils.UserUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    private final JwtProvider jwtProvider;
    private final UserService userService;


    @Operation(summary = "회원가입", description = "사용자가 회원가입을 진행합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/signin")
    public ResponseEntity<CommonResponse<UserResponseDto>> registerUser(@Valid @RequestBody SignInRequestDto dto) {
        UserResponseDto saved = userService.create(dto);
        CommonResponse<UserResponseDto> response = new CommonResponse<>(ResponseMessage.USER_SIGN_IN_SUCCESS, saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "로그인", description = "사용자가 로그인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = JwtAuthResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<JwtAuthResponseDto>> login(@RequestBody LoginRequestDto dto) {
        JwtAuthResponseDto login = userService.login(dto);
        return ResponseEntity.ok(new CommonResponse<>(ResponseMessage.USER_LOG_IN_SUCCESS, login));
    }

    @Operation(summary = "로그아웃", description = "사용자가 로그아웃을 수행합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "로그아웃 실패")
    })
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

    @Operation(summary = "Access Token 갱신", description = "Refresh Token을 이용해 새로운 Access Token을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공",
                    content = @Content(schema = @Schema(implementation = JwtAuthResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "토큰 갱신 실패")
    })
    @PostMapping("/refresh")
    public ResponseEntity<CommonResponse<JwtAuthResponseDto>> refreshAccessToken (@RequestBody RefreshTokenRequestDto requestDto) {
        JwtAuthResponseDto jwtAuthResponseDto = userService.refreshAccessToken(requestDto);
        return ResponseEntity.ok(new CommonResponse<>(ResponseMessage.USER_ACCESS_TOKEN_REFRESHED, jwtAuthResponseDto));
    }

    @Operation(summary = "사용자 정보 조회", description = "로그인한 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserDetailResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<CommonResponse<UserDetailResponseDto>> getUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserDetailResponseDto userDetailResponseDto = userService.getUser(UserUtils.getUser(userDetails));
        CommonResponse<UserDetailResponseDto> response = new CommonResponse<>(ResponseMessage.USER_SELECT_SUCCESS, userDetailResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "사용자 정보 수정", description = "사용자가 자신의 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용자 정보 수정 성공",
                    content = @Content(schema = @Schema(implementation = UserDetailResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PatchMapping
    public ResponseEntity<CommonResponse<UserDetailResponseDto>> updateUser(@AuthenticationPrincipal UserDetails userDetails,
                                                                            @Valid @RequestBody UserUpdateRequestDto userUpdateRequestDto) {
        UserDetailResponseDto userDetailResponseDto = userService.updateUser(UserUtils.getUser(userDetails), userUpdateRequestDto);
        CommonResponse<UserDetailResponseDto> response = new CommonResponse<>(ResponseMessage.USER_SELECT_SUCCESS, userDetailResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "회원 탈퇴", description = "사용자가 회원 탈퇴를 진행합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/leave")
    public ResponseEntity<Void> leave(@RequestBody LeaveRequestDto dto,
                                      @AuthenticationPrincipal UserDetailsImp authenticatedPrincipal) {
        userService.leave(dto.getPassword(), authenticatedPrincipal.getUser());
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
