package com.Soo_Shinsa.global.security;

/**
 * 🔒 보안 로깅 유틸리티
 * 민감한 정보(토큰, 이메일, 개인정보)를 안전하게 마스킹하여 로깅합니다.
 */
public class SecurityLogger {
    
    /**
     * JWT 토큰을 안전하게 마스킹합니다.
     * 예: eyJhbGciOiJIUzI1NiIs... → eyJhb***...s1NiIs
     */
    public static String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 5) + "***" + token.substring(token.length() - 5);
    }
    
    /**
     * 이메일을 안전하게 마스킹합니다.
     * 예: user@example.com → u***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        if (parts[0].length() <= 1) {
            return "***@" + parts[1];
        }
        return parts[0].charAt(0) + "***@" + parts[1];
    }
    
    /**
     * Redis 키에서 토큰 부분을 마스킹합니다.
     * 예: ACCESS_TOKEN:eyJhbGc... → ACCESS_TOKEN:eyJhb***...s1NiIs
     */
    public static String maskRedisKey(String redisKey) {
        if (redisKey == null || !redisKey.contains(":")) {
            return "***";
        }
        String[] parts = redisKey.split(":", 2);
        if (parts.length != 2) {
            return "***";
        }
        return parts[0] + ":" + maskToken(parts[1]);
    }
    
    /**
     * 전화번호를 안전하게 마스킹합니다.
     * 예: 010-1234-5678 → 010-***-5678
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "***";
        }
        
        // 숫자만 추출
        String numbersOnly = phoneNumber.replaceAll("[^0-9]", "");
        if (numbersOnly.length() < 8) {
            return "***";
        }
        
        // 앞 3자리와 뒤 4자리만 표시
        return numbersOnly.substring(0, 3) + "-***-" + 
               numbersOnly.substring(numbersOnly.length() - 4);
    }
    
    /**
     * 사용자 ID를 안전하게 마스킹합니다.
     */
    public static String maskUserId(String userId) {
        if (userId == null || userId.length() <= 2) {
            return "***";
        }
        return userId.charAt(0) + "***" + userId.charAt(userId.length() - 1);
    }
}