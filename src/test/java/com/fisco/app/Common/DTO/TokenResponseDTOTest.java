package com.fisco.app.Common.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenResponseDTO 单元测试
 *
 * 测试覆盖：
 * - UT001-UT003: 字段设置与获取
 * - UT004-UT005: 静态工厂方法 of()
 * - UT006-UT008: Jackson @JsonProperty注解验证
 */
class TokenResponseDTOTest {

    // ==================== 字段设置与获取测试 ====================

    /**
     * UT001: 测试设置和获取accessToken
     */
    @Test
    void testAccessToken() {
        TokenResponseDTO dto = new TokenResponseDTO();
        dto.setAccessToken("testToken");

        assertEquals("testToken", dto.getAccessToken());
    }

    /**
     * UT002: 测试设置和获取refreshToken
     */
    @Test
    void testRefreshToken() {
        TokenResponseDTO dto = new TokenResponseDTO();
        dto.setRefreshToken("refreshToken");

        assertEquals("refreshToken", dto.getRefreshToken());
    }

    /**
     * UT003: 测试设置和获取所有字段
     */
    @Test
    void testAllFields() {
        TokenResponseDTO dto = new TokenResponseDTO();
        dto.setAccessToken("accessToken");
        dto.setRefreshToken("refreshToken");
        dto.setExpiresIn(7200L);
        dto.setTokenType("Bearer");
        dto.setScope("all");
        dto.setUserId(1L);
        dto.setEntId(100L);

        assertEquals("accessToken", dto.getAccessToken());
        assertEquals("refreshToken", dto.getRefreshToken());
        assertEquals(7200L, dto.getExpiresIn());
        assertEquals("Bearer", dto.getTokenType());
        assertEquals("all", dto.getScope());
        assertEquals(1L, dto.getUserId());
        assertEquals(100L, dto.getEntId());
    }

    // ==================== 静态工厂方法测试 ====================

    /**
     * UT004: 测试of()工厂方法
     */
    @Test
    void testOfFactoryMethod() {
        TokenResponseDTO dto = TokenResponseDTO.of(
                "accessToken",
                "refreshToken",
                7200L,
                1L,
                100L
        );

        assertEquals("accessToken", dto.getAccessToken());
        assertEquals("refreshToken", dto.getRefreshToken());
        assertEquals(7200L, dto.getExpiresIn());
        assertEquals("Bearer", dto.getTokenType());
        assertEquals(1L, dto.getUserId());
        assertEquals(100L, dto.getEntId());
    }

    /**
     * UT005: 测试of()工厂方法null值
     */
    @Test
    void testOfFactoryMethodWithNulls() {
        TokenResponseDTO dto = TokenResponseDTO.of(
                null,
                null,
                null,
                null,
                null
        );

        assertNull(dto.getAccessToken());
        assertNull(dto.getRefreshToken());
        assertNull(dto.getExpiresIn());
        assertEquals("Bearer", dto.getTokenType());
        assertNull(dto.getUserId());
        assertNull(dto.getEntId());
    }

    // ==================== @JsonProperty注解验证 ====================

    /**
     * UT006: 验证accessToken字段的@JsonProperty注解
     */
    @Test
    void testJsonPropertyAccessToken() throws NoSuchFieldException {
        // 通过反射验证@JsonProperty注解
        java.lang.reflect.Field field = TokenResponseDTO.class.getDeclaredField("accessToken");
        JsonProperty annotation = field.getAnnotation(JsonProperty.class);

        assertNotNull(annotation);
        assertEquals("accessToken", annotation.value());
    }

    /**
     * UT007: 验证refreshToken字段的@JsonProperty注解
     */
    @Test
    void testJsonPropertyRefreshToken() throws NoSuchFieldException {
        java.lang.reflect.Field field = TokenResponseDTO.class.getDeclaredField("refreshToken");
        JsonProperty annotation = field.getAnnotation(JsonProperty.class);

        assertNotNull(annotation);
        assertEquals("refreshToken", annotation.value());
    }

    /**
     * UT008: 验证expiresIn字段的@JsonProperty注解
     */
    @Test
    void testJsonPropertyExpiresIn() throws NoSuchFieldException {
        java.lang.reflect.Field field = TokenResponseDTO.class.getDeclaredField("expiresIn");
        JsonProperty annotation = field.getAnnotation(JsonProperty.class);

        assertNotNull(annotation);
        assertEquals("expiresIn", annotation.value());
    }

    /**
     * UT009: 验证userId字段的@JsonProperty注解
     */
    @Test
    void testJsonPropertyUserId() throws NoSuchFieldException {
        java.lang.reflect.Field field = TokenResponseDTO.class.getDeclaredField("userId");
        JsonProperty annotation = field.getAnnotation(JsonProperty.class);

        assertNotNull(annotation);
        assertEquals("userId", annotation.value());
    }

    /**
     * UT010: 验证entId字段的@JsonProperty注解
     */
    @Test
    void testJsonPropertyEntId() throws NoSuchFieldException {
        java.lang.reflect.Field field = TokenResponseDTO.class.getDeclaredField("entId");
        JsonProperty annotation = field.getAnnotation(JsonProperty.class);

        assertNotNull(annotation);
        assertEquals("entId", annotation.value());
    }

    /**
     * UT011: 验证scope字段没有@JsonProperty注解（使用默认值）
     */
    @Test
    void testJsonPropertyScope() throws NoSuchFieldException {
        java.lang.reflect.Field field = TokenResponseDTO.class.getDeclaredField("scope");
        JsonProperty annotation = field.getAnnotation(JsonProperty.class);

        // scope字段没有@JsonProperty注解，使用字段名
        assertNull(annotation);
    }
}
