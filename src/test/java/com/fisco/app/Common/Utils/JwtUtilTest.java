package com.fisco.app.Common.Utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 *
 * 测试JWT令牌生成、解析、刷新等核心功能
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JwtUtilTest {

    private static final String TEST_SECRET = "test-jwt-secret-key-for-unit-testing-only-do-not-use-in-production";

    @BeforeAll
    static void setUp() {
        // 初始化JWT配置
        JwtUtil.init(TEST_SECRET, 7200000L, 604800000L);
    }

    // ==================== Token生成测试 ====================

    @Test
    @Order(1)
    @DisplayName("生成AccessToken成功")
    void createAccessToken_shouldSuccess() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "ADMIN";
        Integer scope = 1;

        // Act
        String token = JwtUtil.createAccessToken(sub, entId, role, scope);

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    @Order(2)
    @DisplayName("生成Token对成功")
    void createTokenPair_shouldReturnTokenPair() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "ADMIN";
        Integer scope = 1;

        // Act
        Map<String, String> tokenPair = JwtUtil.createTokenPair(sub, entId, role, scope);

        // Assert
        assertNotNull(tokenPair);
        assertNotNull(tokenPair.get("accessToken"));
        assertNotNull(tokenPair.get("refreshToken"));
        assertNotNull(tokenPair.get("expiresIn"));
    }

    // ==================== Token解析测试 ====================

    @Test
    @Order(10)
    @DisplayName("解析Token成功 - 获取用户ID")
    void parseToken_shouldReturnSubId() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "ADMIN";
        Integer scope = 1;
        String token = JwtUtil.createAccessToken(sub, entId, role, scope);

        // Act
        Claims claims = JwtUtil.parseToken(token);
        Long parsedSub = JwtUtil.getSubId(claims);

        // Assert
        assertEquals(sub, parsedSub);
    }

    @Test
    @Order(11)
    @DisplayName("解析Token成功 - 获取企业ID")
    void parseToken_shouldReturnEntId() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "ADMIN";
        Integer scope = 1;
        String token = JwtUtil.createAccessToken(sub, entId, role, scope);

        // Act
        Claims claims = JwtUtil.parseToken(token);
        Long parsedEntId = JwtUtil.getEntId(claims);

        // Assert
        assertEquals(entId, parsedEntId);
    }

    @Test
    @Order(12)
    @DisplayName("解析Token成功 - 获取角色")
    void parseToken_shouldReturnRole() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "ADMIN";
        Integer scope = 1;
        String token = JwtUtil.createAccessToken(sub, entId, role, scope);

        // Act
        Claims claims = JwtUtil.parseToken(token);
        String parsedRole = JwtUtil.getRole(claims);

        // Assert
        assertEquals(role, parsedRole);
    }

    // ==================== Token验证测试 ====================

    @Test
    @Order(20)
    @DisplayName("验证Token有效")
    void validateToken_shouldReturnTrue_whenValid() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "ADMIN";
        Integer scope = 1;
        String token = JwtUtil.createAccessToken(sub, entId, role, scope);

        // Act
        boolean result = JwtUtil.validateToken(token);

        // Assert
        assertTrue(result);
    }

    @Test
    @Order(21)
    @DisplayName("验证Token失败 - 无效签名")
    void validateToken_shouldReturnFalse_whenInvalidSignature() {
        // Arrange
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature";

        // Act
        boolean result = JwtUtil.validateToken(invalidToken);

        // Assert
        assertFalse(result);
    }

    @Test
    @Order(22)
    @DisplayName("验证Token失败 - 格式错误")
    void validateToken_shouldReturnFalse_whenMalformed() {
        // Arrange
        String malformedToken = "not.a.valid.jwt.token";

        // Act
        boolean result = JwtUtil.validateToken(malformedToken);

        // Assert
        assertFalse(result);
    }

    @Test
    @Order(23)
    @DisplayName("验证Token失败 - 空Token")
    void validateToken_shouldReturnFalse_whenEmpty() {
        // Act
        boolean result = JwtUtil.validateToken("");

        // Assert
        assertFalse(result);
    }

    // ==================== Token类型判断测试 ====================

    @Test
    @Order(30)
    @DisplayName("判断Token类型 - AccessToken")
    void isAccessToken_shouldReturnTrue() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "ADMIN";
        Integer scope = 1;
        String token = JwtUtil.createAccessToken(sub, entId, role, scope);

        // Act
        Claims claims = JwtUtil.parseToken(token);
        boolean isAccess = JwtUtil.isAccessToken(claims);

        // Assert
        assertTrue(isAccess);
    }

    @Test
    @Order(31)
    @DisplayName("判断Token类型 - RefreshToken")
    void isRefreshToken_shouldReturnTrue() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "ADMIN";
        Integer scope = 1;
        String token = JwtUtil.createRefreshToken(sub, entId, role, scope);

        // Act
        Claims claims = JwtUtil.parseToken(token);
        boolean isRefresh = JwtUtil.isRefreshToken(claims);

        // Assert
        assertTrue(isRefresh);
    }

    // ==================== 权限判断测试 ====================

    @Test
    @Order(40)
    @DisplayName("权限等级获取")
    void getPermissionLevel_shouldReturnLevel() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "ADMIN";
        Integer scope = 1;
        String token = JwtUtil.createAccessToken(sub, entId, role, scope);

        // Act
        Claims claims = JwtUtil.parseToken(token);
        int level = JwtUtil.getPermissionLevel(claims);

        // Assert
        assertTrue(level >= 0);
    }

    @Test
    @Order(41)
    @DisplayName("金融角色判断 - ADMIN")
    void isFinanceRole_shouldReturnTrue_forAdmin() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "ADMIN";
        Integer scope = 1;
        String token = JwtUtil.createAccessToken(sub, entId, role, scope);

        // Act
        Claims claims = JwtUtil.parseToken(token);
        boolean isFinance = JwtUtil.isFinanceRole(claims);

        // Assert
        assertTrue(isFinance);
    }

    @Test
    @Order(42)
    @DisplayName("金融角色判断 - OPERATOR")
    void isFinanceRole_shouldReturnFalse_forOperator() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "OPERATOR";
        Integer scope = 0; // 0 = 非系统管理员
        String token = JwtUtil.createAccessToken(sub, entId, role, scope);

        // Act
        Claims claims = JwtUtil.parseToken(token);
        boolean isFinance = JwtUtil.isFinanceRole(claims);

        // Assert
        assertFalse(isFinance);
    }

    // ==================== 数据归属判断测试 ====================

    @Test
    @Order(50)
    @DisplayName("数据归属判断 - 匹配")
    void canAccess_shouldReturnTrue_whenEntIdMatches() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "ADMIN";
        Integer scope = 1;
        String token = JwtUtil.createAccessToken(sub, entId, role, scope);

        // Act
        Claims claims = JwtUtil.parseToken(token);
        boolean canAccess = JwtUtil.canAccess(claims, entId);

        // Assert
        assertTrue(canAccess);
    }

    @Test
    @Order(51)
    @DisplayName("数据归属判断 - 不匹配")
    void canAccess_shouldReturnFalse_whenEntIdNotMatches() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "ADMIN";
        Integer scope = 0; // 0 = 非系统管理员，需要检查entId
        String token = JwtUtil.createAccessToken(sub, entId, role, scope);

        // Act
        Claims claims = JwtUtil.parseToken(token);
        boolean canAccess = JwtUtil.canAccess(claims, 9999L);

        // Assert
        assertFalse(canAccess);
    }

    // ==================== 边界值测试 ====================

    @Test
    @Order(60)
    @DisplayName("生成Token - 特殊角色")
    void createToken_shouldHandleSpecialRoles() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "FINANCE";
        Integer scope = 2;

        // Act
        String token = JwtUtil.createAccessToken(sub, entId, role, scope);

        // Assert
        assertNotNull(token);
        assertTrue(JwtUtil.validateToken(token));
    }

    @Test
    @Order(61)
    @DisplayName("生成Token - 零值scope")
    void createToken_shouldHandleZeroScope() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "OPERATOR";
        Integer scope = 0;

        // Act
        String token = JwtUtil.createAccessToken(sub, entId, role, scope);

        // Assert
        assertNotNull(token);
    }

    @Test
    @Order(62)
    @DisplayName("解析Token - 包含额外Claims")
    void parseToken_shouldHandleExtraClaims() {
        // Arrange
        Long sub = 1001L;
        Long entId = 2001L;
        String role = "ADMIN";
        Integer scope = 1;

        // 创建带额外claims的token
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("sub", sub.toString()); // JWT要求subject必须是String
        claimsMap.put("entId", entId);
        claimsMap.put("role", role);
        claimsMap.put("scope", scope);
        claimsMap.put("tokenType", "access");
        claimsMap.put("customClaim", "customValue");
        claimsMap.put("jti", "unique-id-123");

        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .claims(claimsMap)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 7200000))
                .signWith(key)
                .compact();

        // Act
        Claims parsed = JwtUtil.parseToken(token);

        // Assert
        assertEquals("customValue", parsed.get("customClaim"));
    }
}
