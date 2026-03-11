package com.fisco.app.Common.Service.impl;

import com.fisco.app.Common.Utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenServiceImpl单元测试 - 双令牌策略
 */
class TokenServiceImplTest {

    private TokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        // JwtUtil 使用静态配置，已有默认值
        tokenService = new TokenServiceImpl();
    }

    @Test
    void testGenerateTokenPair() {
        // 测试生成令牌对
        Map<String, String> tokenPair = tokenService.generateTokenPair(1001L, 100L, "ADMIN", 1);

        assertNotNull(tokenPair);
        assertNotNull(tokenPair.get("accessToken"));
        assertNotNull(tokenPair.get("refreshToken"));
    }

    @Test
    void testGenerateTokenPairWithNullUserId() {
        // 测试userId为null时的异常（被包装为RuntimeException）
        assertThrows(RuntimeException.class, () -> {
            tokenService.generateTokenPair(null, 100L, "ADMIN", 1);
        });
    }

    @Test
    void testRefreshToken() {
        // 先生成一个有效的refreshToken
        Map<String, String> tokenPair = tokenService.generateTokenPair(1001L, 100L, "ADMIN", 1);
        String refreshToken = tokenPair.get("refreshToken");

        // 测试刷新Token
        Map<String, String> newTokenPair = tokenService.refreshToken(refreshToken);

        assertNotNull(newTokenPair);
        assertNotNull(newTokenPair.get("accessToken"));
        assertNotNull(newTokenPair.get("refreshToken"));
    }

    @Test
    void testRefreshTokenWithInvalidToken() {
        // 测试使用无效Token刷新
        Map<String, String> newTokenPair = tokenService.refreshToken("invalid.token");

        assertNull(newTokenPair);
    }

    @Test
    void testRefreshTokenWithAccessToken() {
        // 测试使用Access Token刷新（应该失败）
        Map<String, String> tokenPair = tokenService.generateTokenPair(1001L, 100L, "ADMIN", 1);
        String accessToken = tokenPair.get("accessToken");

        Map<String, String> newTokenPair = tokenService.refreshToken(accessToken);

        assertNull(newTokenPair);
    }

    @Test
    void testValidateAccessToken() {
        // 生成有效的accessToken并验证
        Map<String, String> tokenPair = tokenService.generateTokenPair(1001L, 100L, "ADMIN", 1);
        String accessToken = tokenPair.get("accessToken");

        assertTrue(tokenService.validateAccessToken(accessToken));
    }

    @Test
    void testValidateInvalidAccessToken() {
        // 测试验证无效Token
        assertFalse(tokenService.validateAccessToken("invalid.token"));
    }

    @Test
    void testParseAccessToken() {
        // 测试解析Access Token
        Map<String, String> tokenPair = tokenService.generateTokenPair(1001L, 100L, "ADMIN", 1);
        String accessToken = tokenPair.get("accessToken");

        Map<String, Object> userInfo = tokenService.parseAccessToken(accessToken);

        assertNotNull(userInfo);
        assertEquals(1001L, userInfo.get("userId"));
        assertEquals(100L, userInfo.get("entId"));
        assertEquals("ADMIN", userInfo.get("role"));
        assertEquals(1, userInfo.get("scope"));
        assertNotNull(userInfo.get("jti"));
        assertNotNull(userInfo.get("expireIn"));
    }

    @Test
    void testRevokeToken() {
        // 生成Token并吊销
        Map<String, String> tokenPair = tokenService.generateTokenPair(1001L, 100L, "ADMIN", 1);
        String accessToken = tokenPair.get("accessToken");

        // 吊销Token
        assertTrue(tokenService.revokeToken(accessToken));

        // 验证吊销后的Token
        assertFalse(tokenService.validateAccessToken(accessToken));
    }

    @Test
    void testRevokeTokenWithInvalidToken() {
        // 测试吊销无效Token
        boolean result = tokenService.revokeToken("invalid.token");

        assertFalse(result);
    }

    @Test
    void testIsTokenBlacklisted() {
        // 测试黑名单查询
        // 先生成一个Token
        Map<String, String> tokenPair = tokenService.generateTokenPair(1001L, 100L, "ADMIN", 1);
        String accessToken = tokenPair.get("accessToken");

        // 初始不在黑名单
        String jti = JwtUtil.parseToken(accessToken).get("jti", String.class);
        assertFalse(tokenService.isTokenBlacklisted(jti));

        // 吊销后加入黑名单
        tokenService.revokeToken(accessToken);

        // 验证在黑名单中
        assertTrue(tokenService.isTokenBlacklisted(jti));
    }

    @Test
    void testIsTokenBlacklistedWithNullJti() {
        // 测试null JTI
        assertFalse(tokenService.isTokenBlacklisted(null));
    }
}
