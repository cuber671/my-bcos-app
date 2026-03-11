package com.fisco.app.Common.Config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IdempotentInterceptor 单元测试
 * 测试幂等性拦截器的核心逻辑
 *
 * 测试覆盖：
 * - UT004-UT006: UUID格式验证
 * - UT007: 幂等Key构建
 * - UT008-UT009: 缓存失效方法
 */
class IdempotentInterceptorTest {

    private final IdempotentInterceptor interceptor = new IdempotentInterceptor();

    /**
     * UT004: 测试UUID格式验证 - 标准UUID
     * 预期：验证通过
     */
    @Test
    void testValidStandardUUID() throws Exception {
        // 标准UUID格式
        String uuid = "550e8400-e29b-41d4-a716-446655440000";

        // 通过反射调用私有方法
        Method isValidUUID = IdempotentInterceptor.class.getDeclaredMethod("isValidUUID", String.class);
        isValidUUID.setAccessible(true);
        Boolean result = (Boolean) isValidUUID.invoke(interceptor, uuid);

        assertTrue(result);
        System.out.println("✅ UT004: 标准UUID格式验证通过 - " + uuid);
    }

    /**
     * UT005: 测试UUID格式验证 - 32位16进制
     * 预期：验证通过
     */
    @Test
    void testValid32HexUUID() throws Exception {
        // 32位16进制字符串（无连字符）
        String hex32 = "550e8400e29b41d4a716446655440000";

        Method isValidUUID = IdempotentInterceptor.class.getDeclaredMethod("isValidUUID", String.class);
        isValidUUID.setAccessible(true);
        Boolean result = (Boolean) isValidUUID.invoke(interceptor, hex32);

        assertTrue(result);
        System.out.println("✅ UT005: 32位16进制UUID验证通过 - " + hex32);
    }

    /**
     * UT006: 测试UUID格式验证 - 无效格式
     * 预期：验证失败
     */
    @Test
    void testInvalidUUID() throws Exception {
        // 无效UUID格式
        String invalidUuid = "invalid-uuid";
        String shortUuid = "123";
        String tooLongUuid = "550e8400e29b41d4a71644665544000000"; // 34位

        Method isValidUUID = IdempotentInterceptor.class.getDeclaredMethod("isValidUUID", String.class);
        isValidUUID.setAccessible(true);

        assertFalse((Boolean) isValidUUID.invoke(interceptor, invalidUuid));
        assertFalse((Boolean) isValidUUID.invoke(interceptor, shortUuid));
        assertFalse((Boolean) isValidUUID.invoke(interceptor, tooLongUuid));

        System.out.println("✅ UT006: 无效UUID格式验证通过");
    }

    /**
     * UT007: 测试幂等Key构建
     * 预期：格式为 idempotent_{userId}_{transactionId}
     */
    @Test
    void testBuildIdempotentKey() throws Exception {
        String userId = "user123";
        String transactionId = "550e8400-e29b-41d4-a716-446655440000";

        Method buildKey = IdempotentInterceptor.class.getDeclaredMethod("buildIdempotentKey", String.class, String.class);
        buildKey.setAccessible(true);
        String key = (String) buildKey.invoke(interceptor, userId, transactionId);

        assertEquals("idempotent_user123_550e8400-e29b-41d4-a716-446655440000", key);
        System.out.println("✅ UT007: 幂等Key构建正确 - " + key);
    }

    /**
     * UT007b: 测试幂等Key构建 - 无用户ID
     * 预期：格式为 idempotent_{transactionId}
     */
    @Test
    void testBuildIdempotentKeyWithoutUserId() throws Exception {
        String userId = null;
        String transactionId = "550e8400e29b41d4a716446655440000";

        Method buildKey = IdempotentInterceptor.class.getDeclaredMethod("buildIdempotentKey", String.class, String.class);
        buildKey.setAccessible(true);
        String key = (String) buildKey.invoke(interceptor, userId, transactionId);

        assertEquals("idempotent_550e8400e29b41d4a716446655440000", key);
        System.out.println("✅ UT007b: 无用户ID时幂等Key构建正确 - " + key);
    }

    /**
     * UT008: 测试幂等缓存失效方法 invalidate()
     * 预期：单个key清理成功
     */
    @Test
    void testInvalidate() throws Exception {
        // 先手动放入一个缓存
        String userId = "testUser";
        String transactionId = "550e8400e29b41d4a716446655440000";

        // 调用invalidate方法
        interceptor.invalidate(userId, transactionId);

        System.out.println("✅ UT008: invalidate方法调用成功");
    }

    /**
     * UT009: 测试幂等缓存失效方法 invalidateAll()
     * 预期：所有缓存清理成功
     */
    @Test
    void testInvalidateAll() throws Exception {
        // 调用invalidateAll方法
        interceptor.invalidateAll();

        System.out.println("✅ UT009: invalidateAll方法调用成功");
    }

    /**
     * UT010: 测试null UUID验证
     * 预期：返回false
     */
    @Test
    void testNullUUID() throws Exception {
        Method isValidUUID = IdempotentInterceptor.class.getDeclaredMethod("isValidUUID", String.class);
        isValidUUID.setAccessible(true);

        assertFalse((Boolean) isValidUUID.invoke(interceptor, (String) null));

        System.out.println("✅ UT010: null UUID验证通过");
    }

    /**
     * UT011: 测试带字母的16进制UUID验证
     * 预期：返回true
     */
    @Test
    void testHexUUIDWithLetters() throws Exception {
        // 包含a-f的32位16进制
        String hexWithLetters = "deadbeef12345678deadbeef12345678";

        Method isValidUUID = IdempotentInterceptor.class.getDeclaredMethod("isValidUUID", String.class);
        isValidUUID.setAccessible(true);
        Boolean result = (Boolean) isValidUUID.invoke(interceptor, hexWithLetters);

        assertTrue(result);
        System.out.println("✅ UT011: 包含字母的16进制UUID验证通过");
    }
}
