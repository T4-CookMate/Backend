package com.cookmate.orchestrator.Common.Security.Jwt;

import com.cookmate.orchestrator.Common.ApiPayload.Status.ErrorStatus;
import com.cookmate.orchestrator.Common.Exception.GeneralException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    /**
     * 서명에 쓸 Key
     */
    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * access token 생성
     */
    public String generateAccessToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * refresh token 생성
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰 유효성 검사
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException | io.jsonwebtoken.security.SignatureException e) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN.getMessage());
        } catch (ExpiredJwtException e) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED, "토큰 만료");
        } catch (UnsupportedJwtException e) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN.getMessage());
        } catch (IllegalArgumentException e) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        } catch (Exception e) {
            log.warn("Unexpected JWT error: {}", e.getMessage());
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 토큰에서 userId 꺼내기
     */
    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
