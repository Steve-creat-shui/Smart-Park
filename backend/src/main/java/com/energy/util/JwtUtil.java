package com.energy.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret:energy-twin-secret-key-2024-very-long-string}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private SecretKey getKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            return Keys.hmacShaKeyFor(padded);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String phone, String nickname, String role) {
        return generateToken(phone, nickname, role, "normal", null);
    }

    public String generateToken(String phone, String nickname, String role, String userType, Long enterpriseId) {
        return generateToken(phone, nickname, role, userType, enterpriseId, null);
    }

    public String generateToken(String phone, String nickname, String role, String userType, Long enterpriseId, Long userId) {
        var builder = Jwts.builder()
                .setSubject(phone)
                .claim("nickname", nickname)
                .claim("role", role)
                .claim("userType", userType != null ? userType : "normal")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey());
        if (enterpriseId != null) {
            builder.claim("enterpriseId", enterpriseId);
        }
        if (userId != null) {
            builder.claim("userId", userId);
        }
        return builder.compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getPhoneFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public String getUserTypeFromToken(String token) {
        return parseToken(token).get("userType", String.class);
    }

    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    public Long getEnterpriseIdFromToken(String token) {
        Object val = parseToken(token).get("enterpriseId");
        if (val instanceof Number) return ((Number) val).longValue();
        return null;
    }

    public Long getUserIdFromToken(String token) {
        Object val = parseToken(token).get("userId");
        if (val instanceof Number) return ((Number) val).longValue();
        return null;
    }
}
