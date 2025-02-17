package com.Soo_Shinsa.auth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JwtAuthResponseDto {

  private String tokenAuthScheme;
  private String refreshToken;
  private Long refreshTokenExpiration;
  private String accessToken;
  private String email;

  @Builder
  public JwtAuthResponseDto(String tokenAuthScheme, String refreshToken, Long refreshTokenExpiration, String accessToken, String email) {
    this.tokenAuthScheme = tokenAuthScheme;
    this.refreshToken = refreshToken;
    this.refreshTokenExpiration = refreshTokenExpiration;
    this.accessToken = accessToken;
    this.email = email;
  }
}
