package com.Soo_Shinsa.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Soo_Shinsa API", version = "1.0", description = "Soo_Shinsa 프로젝트 API 명세서"),
        security = {  // ✅ BearerAuth도 여기에 추가!
                @SecurityRequirement(name = "BearerAuth"),
                @SecurityRequirement(name = "KakaoOAuth2"),
                @SecurityRequirement(name = "KakaoClientCredentials")
        }
)
@SecurityScheme(
        name = "BearerAuth",  // ✅ JWT 인증 추가
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
@SecurityScheme(
        name = "KakaoOAuth2",  // ✅ Kakao OAuth2 설정 유지
        type = SecuritySchemeType.OAUTH2,
        in = SecuritySchemeIn.HEADER,
        bearerFormat = "JWT",
        flows = @OAuthFlows(
                authorizationCode = @OAuthFlow(
                        authorizationUrl = "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=6d2b0e6f025085e4d270ed4cb78a6f64&redirect_uri=http://localhost:8080/callback&prompt=login",
                        tokenUrl = "https://kauth.kakao.com/oauth/token"
                )
        )
)
public class OpenAPIConfig {
}
