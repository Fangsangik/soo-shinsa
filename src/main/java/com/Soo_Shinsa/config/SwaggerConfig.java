package com.Soo_Shinsa.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API 문서")  // ✅ API 문서 제목
                        .description("Swagger를 이용한 API 문서")  // ✅ API 설명
                        .version("1.0.0")  // ✅ 버전
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))  // ✅ 라이선스 정보
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))  // ✅ JWT 인증 추가
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .name("BearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
