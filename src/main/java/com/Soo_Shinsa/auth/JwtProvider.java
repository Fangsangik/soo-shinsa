package com.Soo_Shinsa.auth;

import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

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

    private final UserRepository userRepository;
    private final JwtBlackListService jwtBlackListService;

    public String generateToken(Authentication authentication) throws EntityNotFoundException {
        return generateTokenBy(authentication.getName(), expiryMillis);
    }

    public String generateRefreshToken(String email) {
        return generateTokenBy(email, refreshExpiryMillis);
    }

    public String getUsername(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    public String generateTokenBy(String email, long expiration) throws EntityNotFoundException {
        User user = userRepository.findByEmailOrElseThrow(email);
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + expiration);

        return Jwts.builder()
                .subject(user.getEmail())
                .id(java.util.UUID.randomUUID().toString()) // 같은 초에 재로그인해도 토큰이 달라지도록
                .issuedAt(currentDate)
                .expiration(expireDate)
                .claim("role", user.getRole())
                .claim("uid", user.getUserId())
                .claim("name", user.getName())
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    public boolean validAccessToken(String token) {
        if (jwtBlackListService.isBlackListed(token)) {
            log.warn("Token is blacklisted: {}", token);
            return false;
        }
        return validToken(token);
    }

    /** null 이면 정상. 만료면 TOKEN_EXPIRED, 서명 불일치·형식 오류면 INVALID_TOKEN */
    public com.Soo_Shinsa.exception.ErrorCode check(String token) {
        try {
            Claims c = getClaims(token);
            // uid/role 클레임이 없는 토큰(이전 버전 발급분)은 인증 주체를 만들 수 없으므로 재로그인 유도
            return c.get("uid") == null || c.get("role") == null ? com.Soo_Shinsa.exception.ErrorCode.INVALID_TOKEN : null;
        } catch (ExpiredJwtException e) {
            return com.Soo_Shinsa.exception.ErrorCode.TOKEN_EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            return com.Soo_Shinsa.exception.ErrorCode.INVALID_TOKEN;
        }
    }

    public boolean validToken(String token) {
        try {
            return !tokenExpired(token);
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }


    /** 토큰 클레임으로 인증 주체 생성 (DB 조회 없음) */
    public User principalFrom(String token) {
        Claims c = getClaims(token);
        return User.principal(c.get("uid", Long.class), c.getSubject(), c.get("name", String.class), Role.valueOf(String.valueOf(c.get("role"))));
    }

    /** 토큰 만료까지 남은 ms (블랙리스트 TTL 용) */
    public long remainingMillis(String token) {
        return Math.max(1, getExpirationDateFromToken(token).getTime() - System.currentTimeMillis());
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

    private boolean tokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    private Date getExpirationDateFromToken(String token) {
        return resolveClaims(token, Claims::getExpiration);
    }

    private <T> T resolveClaims(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(getClaims(token));
    }
}
