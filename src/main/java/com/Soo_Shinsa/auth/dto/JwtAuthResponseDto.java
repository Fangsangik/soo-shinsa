package com.Soo_Shinsa.auth.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
public class JwtAuthResponseDto {

  private String tokenAuthScheme;
  private String refreshToken;
  private Long refreshTokenExpiration;
  private String accessToken;
  private String username;
}
