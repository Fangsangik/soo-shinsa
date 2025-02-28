package com.Soo_Shinsa.global.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 제공자.
 * <p>토큰의 생성, 추출, 만료 확인 등의 기능.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiry-millis}")
    private long expiryMillis;

    @Value("${jwt.refresh-expiry-millis}")
    private long refreshExpiryMillis;

    private final JwtBlackListService jwtBlackListService;
    private final JwtAccessTokenService jwtAccessTokenService;

    public String generateTokenBy(String email, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validAccessToken(String token) {
        return !jwtBlackListService.isBlackListed(token) && validToken(token);
    }

    public boolean validToken(String token) {
        try {
            return !getClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaims(String token) {
        if (!StringUtils.hasText(token)) {
            throw new MalformedJwtException("토큰이 비어 있습니다.");
        }

        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

