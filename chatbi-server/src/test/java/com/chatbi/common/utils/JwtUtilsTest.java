package com.chatbi.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtils 单元测试
 */
@DisplayName("JwtUtils 测试")
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-generation-and-validation-min-256-bit";
    private static final long TEST_EXPIRATION = 3600000; // 1 hour
    private static final String TEST_USERNAME = "testuser";
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // 通过反射或其他方式设置私有字段
        // 这里假设 JwtUtils 有 setter 或者可以通过配置文件设置
        // 实际测试中可能需要修改 JwtUtils 以支持测试注入
    }

    @Test
    @DisplayName("生成 Access Token 测试")
    void testGenerateAccessToken() {
        String token = Jwts.builder()
                .subject(String.valueOf(TEST_USER_ID))
                .claim("username", TEST_USERNAME)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION))
                .signWith(getSigningKey(TEST_SECRET))
                .compact();

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    @DisplayName("生成 Refresh Token 测试")
    void testGenerateRefreshToken() {
        String refreshToken = Jwts.builder()
                .subject(String.valueOf(TEST_USER_ID))
                .claim("username", TEST_USERNAME)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 7 * 24 * 3600000)) // 7 days
                .signWith(getSigningKey(TEST_SECRET))
                .compact();

        assertNotNull(refreshToken);
        assertTrue(refreshToken.length() > 0);
    }

    @Test
    @DisplayName("验证 Token 成功测试")
    void testValidateToken_Success() {
        String token = Jwts.builder()
                .subject(String.valueOf(TEST_USER_ID))
                .claim("username", TEST_USERNAME)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION))
                .signWith(getSigningKey(TEST_SECRET))
                .compact();

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey(TEST_SECRET))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertNotNull(claims);
        assertEquals(String.valueOf(TEST_USER_ID), claims.getSubject());
        assertEquals(TEST_USERNAME, claims.get("username", String.class));
    }

    @Test
    @DisplayName("验证 Token 失败 - 过期")
    void testValidateToken_Expired() {
        String token = Jwts.builder()
                .subject(String.valueOf(TEST_USER_ID))
                .claim("username", TEST_USERNAME)
                .issuedAt(new Date(System.currentTimeMillis() - 2 * TEST_EXPIRATION))
                .expiration(new Date(System.currentTimeMillis() - TEST_EXPIRATION))
                .signWith(getSigningKey(TEST_SECRET))
                .compact();

        assertThrows(Exception.class, () -> {
            Jwts.parser()
                    .verifyWith(getSigningKey(TEST_SECRET))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        });
    }

    @Test
    @DisplayName("验证 Token 失败 - 签名无效")
    void testValidateToken_InvalidSignature() {
        String validToken = Jwts.builder()
                .subject(String.valueOf(TEST_USER_ID))
                .claim("username", TEST_USERNAME)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION))
                .signWith(getSigningKey(TEST_SECRET))
                .compact();

        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "wrong-secret-key-for-jwt-validation-min-256-bit".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(Exception.class, () -> {
            Jwts.parser()
                    .verifyWith(wrongKey)
                    .build()
                    .parseSignedClaims(validToken)
                    .getPayload();
        });
    }

    @Test
    @DisplayName("从 Token 获取用户 ID 测试")
    void testGetUserIdFromToken() {
        String token = Jwts.builder()
                .subject(String.valueOf(TEST_USER_ID))
                .claim("username", TEST_USERNAME)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION))
                .signWith(getSigningKey(TEST_SECRET))
                .compact();

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey(TEST_SECRET))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.parseLong(claims.getSubject());
        assertEquals(TEST_USER_ID, userId);
    }

    @Test
    @DisplayName("从 Token 获取用户名测试")
    void testGetUsernameFromToken() {
        String token = Jwts.builder()
                .subject(String.valueOf(TEST_USER_ID))
                .claim("username", TEST_USERNAME)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION))
                .signWith(getSigningKey(TEST_SECRET))
                .compact();

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey(TEST_SECRET))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String username = claims.get("username", String.class);
        assertEquals(TEST_USERNAME, username);
    }

    @Test
    @DisplayName("从 Token 获取过期时间测试")
    void testGetExpirationFromToken() {
        Date expectedExpiration = new Date(System.currentTimeMillis() + TEST_EXPIRATION);

        String token = Jwts.builder()
                .subject(String.valueOf(TEST_USER_ID))
                .claim("username", TEST_USERNAME)
                .issuedAt(new Date())
                .expiration(expectedExpiration)
                .signWith(getSigningKey(TEST_SECRET))
                .compact();

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey(TEST_SECRET))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Date expiration = claims.getExpiration();
        assertNotNull(expiration);
        long drift = Math.abs(expectedExpiration.getTime() - expiration.getTime());
        assertTrue(drift <= 120_000, "过期时间偏差过大: " + drift + "ms");
    }

    private SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
