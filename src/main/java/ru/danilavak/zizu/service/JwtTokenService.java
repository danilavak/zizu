package ru.danilavak.zizu.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import ru.danilavak.zizu.model.UserAccount;

@Service
public class JwtTokenService {
    private final SecretKey secretKey;
    private final Duration accessTokenLifetime;
    private final Duration refreshTokenLifetime;

    public JwtTokenService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-minutes}") long accessTokenMinutes,
            @Value("${app.jwt.refresh-token-days}") long refreshTokenDays
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenLifetime = Duration.ofMinutes(accessTokenMinutes);
        this.refreshTokenLifetime = Duration.ofDays(refreshTokenDays);
    }

    public TokenWithExpiry createAccessToken(UserAccount user, Long sessionId) {
        Instant expiresAt = Instant.now().plus(accessTokenLifetime);
        String token = Jwts.builder()
                .subject(user.getUsername())
                .claim("type", "ACCESS")
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .claim("sid", sessionId)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
        return new TokenWithExpiry(token, expiresAt);
    }

    public TokenWithExpiry createRefreshToken(UserAccount user, Long sessionId, String refreshTokenId) {
        Instant expiresAt = Instant.now().plus(refreshTokenLifetime);
        String token = Jwts.builder()
                .subject(user.getUsername())
                .claim("type", "REFRESH")
                .claim("uid", user.getId())
                .claim("sid", sessionId)
                .claim("rid", refreshTokenId)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
        return new TokenWithExpiry(token, expiresAt);
    }

    public AccessTokenPayload parseAccessToken(String token) {
        Claims claims = parse(token);
        requireType(claims, "ACCESS");
        return new AccessTokenPayload(
                claims.getSubject(),
                claims.get("role", String.class),
                claims.get("uid", Long.class),
                claims.get("sid", Long.class)
        );
    }

    public RefreshTokenPayload parseRefreshToken(String token) {
        Claims claims = parse(token);
        requireType(claims, "REFRESH");
        return new RefreshTokenPayload(
                claims.getSubject(),
                claims.get("uid", Long.class),
                claims.get("sid", Long.class),
                claims.get("rid", String.class)
        );
    }

    public Duration getRefreshTokenLifetime() {
        return refreshTokenLifetime;
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void requireType(Claims claims, String expectedType) {
        String actualType = claims.get("type", String.class);
        if (!expectedType.equals(actualType)) {
            throw new JwtException("Invalid token type");
        }
    }

    public record TokenWithExpiry(String token, Instant expiresAt) {
    }

    public record AccessTokenPayload(String username, String role, Long userId, Long sessionId) {
    }

    public record RefreshTokenPayload(String username, Long userId, Long sessionId, String refreshTokenId) {
    }
}
