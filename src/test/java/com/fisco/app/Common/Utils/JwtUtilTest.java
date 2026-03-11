package com.fisco.app.Common.Utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil单元测试 - 双令牌策略
 */
class JwtUtilTest {

    @BeforeEach
    void setUp() {
        // JwtUtil 使用静态配置，已有默认值
    }

    @Test
    void testCreateTokenPair() {
        // 测试生成双令牌
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, 100L, "ADMIN", 1);

        assertNotNull(tokenPair);
        assertNotNull(tokenPair.get("accessToken"));
        assertNotNull(tokenPair.get("refreshToken"));
        assertNotEquals(tokenPair.get("accessToken"), tokenPair.get("refreshToken"));
    }

    @Test
    void testParseToken() {
        // 生成Token并解析
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, 100L, "ADMIN", 1);

        io.jsonwebtoken.Claims accessClaims = JwtUtil.parseToken(tokenPair.get("accessToken"));
        assertNotNull(accessClaims);
        assertEquals("1001", accessClaims.getSubject());
    }

    @Test
    void testValidateToken() {
        // 测试有效Token验证
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, 100L, "ADMIN", 1);

        assertTrue(JwtUtil.validateToken(tokenPair.get("accessToken")));
    }

    @Test
    void testValidateInvalidToken() {
        // 测试无效Token验证
        assertFalse(JwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void testIsAccessToken() {
        // 测试Access Token类型判断
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, 100L, "ADMIN", 1);
        io.jsonwebtoken.Claims claims = JwtUtil.parseToken(tokenPair.get("accessToken"));

        assertTrue(JwtUtil.isAccessToken(claims));
        assertFalse(JwtUtil.isRefreshToken(claims));
    }

    @Test
    void testIsRefreshToken() {
        // 测试Refresh Token类型判断
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, 100L, "ADMIN", 1);
        io.jsonwebtoken.Claims claims = JwtUtil.parseToken(tokenPair.get("refreshToken"));

        assertTrue(JwtUtil.isRefreshToken(claims));
        assertFalse(JwtUtil.isAccessToken(claims));
    }

    @Test
    void testGetSubId() {
        // 测试获取用户ID
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, 100L, "ADMIN", 1);
        io.jsonwebtoken.Claims claims = JwtUtil.parseToken(tokenPair.get("accessToken"));

        assertEquals(1001L, JwtUtil.getSubId(claims));
    }

    @Test
    void testGetEntId() {
        // 测试获取企业ID
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, 100L, "ADMIN", 1);
        io.jsonwebtoken.Claims claims = JwtUtil.parseToken(tokenPair.get("accessToken"));

        assertEquals(100L, JwtUtil.getEntId(claims));
    }

    @Test
    void testGetRole() {
        // 测试获取角色
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, 100L, "ADMIN", 1);
        io.jsonwebtoken.Claims claims = JwtUtil.parseToken(tokenPair.get("accessToken"));

        assertEquals("ADMIN", JwtUtil.getRole(claims));
    }

    @Test
    void testGetScope() {
        // 测试获取权限范围
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, 100L, "ADMIN", 1);
        io.jsonwebtoken.Claims claims = JwtUtil.parseToken(tokenPair.get("accessToken"));

        assertEquals(1, JwtUtil.getScope(claims));
    }

    @Test
    void testGetJti() {
        // 测试获取JTI
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, 100L, "ADMIN", 1);
        io.jsonwebtoken.Claims claims = JwtUtil.parseToken(tokenPair.get("accessToken"));

        assertNotNull(JwtUtil.getJti(claims));
    }

    @Test
    void testCanAccess() {
        // 测试数据访问权限
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, 100L, "USER", 0);
        io.jsonwebtoken.Claims claims = JwtUtil.parseToken(tokenPair.get("accessToken"));

        // 同一企业可访问
        assertTrue(JwtUtil.canAccess(claims, 100L));
        // 不同企业不可访问
        assertFalse(JwtUtil.canAccess(claims, 200L));
    }

    @Test
    void testCanAccessAdmin() {
        // 测试管理员全局访问权限
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, 100L, "ADMIN", 1);
        io.jsonwebtoken.Claims claims = JwtUtil.parseToken(tokenPair.get("accessToken"));

        // 管理员可访问任意企业
        assertTrue(JwtUtil.canAccess(claims, 999L));
    }

    @Test
    void testIsFinanceRole() {
        // 测试金融角色判断
        Map<String, String> tokenPairAdmin = JwtUtil.createTokenPair(1001L, 100L, "ADMIN", 1);
        io.jsonwebtoken.Claims claimsAdmin = JwtUtil.parseToken(tokenPairAdmin.get("accessToken"));

        Map<String, String> tokenPairFinance = JwtUtil.createTokenPair(1002L, 100L, "FINANCE", 0);
        io.jsonwebtoken.Claims claimsFinance = JwtUtil.parseToken(tokenPairFinance.get("accessToken"));

        Map<String, String> tokenPairUser = JwtUtil.createTokenPair(1003L, 100L, "USER", 0);
        io.jsonwebtoken.Claims claimsUser = JwtUtil.parseToken(tokenPairUser.get("accessToken"));

        assertTrue(JwtUtil.isFinanceRole(claimsAdmin));
        assertTrue(JwtUtil.isFinanceRole(claimsFinance));
        assertFalse(JwtUtil.isFinanceRole(claimsUser));
    }

    @Test
    void testCanExecuteFinance() {
        // 测试金融操作权限
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, 100L, "FINANCE", 0);
        io.jsonwebtoken.Claims claims = JwtUtil.parseToken(tokenPair.get("accessToken"));

        assertTrue(JwtUtil.canExecuteFinance(claims, 100L));
        assertFalse(JwtUtil.canExecuteFinance(claims, 200L));
    }

    @Test
    void testGetExpireIn() {
        // 测试获取过期剩余时间
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, 100L, "ADMIN", 1);

        long expireIn = JwtUtil.getExpireIn(tokenPair.get("accessToken"));
        assertTrue(expireIn > 0);
        assertTrue(expireIn <= 7200); // 2小时=7200秒
    }

    @Test
    void testNullEntId() {
        // 测试null企业ID
        Map<String, String> tokenPair = JwtUtil.createTokenPair(1001L, null, "USER", 0);
        io.jsonwebtoken.Claims claims = JwtUtil.parseToken(tokenPair.get("accessToken"));

        assertNull(JwtUtil.getEntId(claims));
    }
}
