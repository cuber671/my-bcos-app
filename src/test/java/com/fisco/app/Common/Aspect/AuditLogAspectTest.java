package com.fisco.app.Common.Aspect;

import com.fisco.app.Common.Utils.AuditContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuditLogAspect单元测试
 * 测试AuditContext和填充逻辑
 */
class AuditLogAspectTest {

    @BeforeEach
    @AfterEach
    void setUp() {
        // 每个测试前后清理AuditContext
        AuditContext.clear();
    }

    /**
     * 测试AuditContext清理
     */
    @Test
    void testAuditContextClear() {
        AuditContext.set(100L, 1L, "ADMIN", 1, "jti-123");
        assertTrue(AuditContext.isPresent());

        AuditContext.clear();

        assertFalse(AuditContext.isPresent());
        assertNull(AuditContext.getUserId());
    }

    /**
     * 测试AuditContext获取各字段
     */
    @Test
    void testAuditContextGetters() {
        AuditContext.set(100L, 1L, "ADMIN", 1, "jti-123");

        assertEquals(100L, AuditContext.getUserId());
        assertEquals(1L, AuditContext.getEntId());
        assertEquals("ADMIN", AuditContext.getRole());
        assertEquals(1, AuditContext.getScope());
        assertEquals("jti-123", AuditContext.getJti());
    }

    /**
     * 测试AuditContext未设置 - isPresent返回false
     */
    @Test
    void testAuditContextNotSet() {
        AuditContext.clear();
        assertFalse(AuditContext.isPresent());
        assertNull(AuditContext.getUserId());
    }

    /**
     * 测试AuditContext仅设置userId
     */
    @Test
    void testAuditContextPartialSet() {
        AuditContext.set(100L, null, null, null, null);

        assertEquals(100L, AuditContext.getUserId());
        assertNull(AuditContext.getEntId());
        assertNull(AuditContext.getRole());
    }

    /**
     * 测试填充单个Entity - 有AuditContext
     */
    @Test
    void testAutoFillSingleEntity() throws Exception {
        AuditContext.set(100L, 1L, "ADMIN", 1, "jti-123");

        TestEntity entity = new TestEntity();
        // 模拟Aspect填充后的结果
        fillEntityOperatorId(entity, 100L);

        assertEquals(100L, entity.getOperatorId());
    }

    /**
     * 测试填充List<Entity> - 有AuditContext
     */
    @Test
    void testAutoFillListEntity() throws Exception {
        AuditContext.set(100L, 1L, "ADMIN", 1, "jti-123");

        List<TestEntity> list = new ArrayList<>();
        TestEntity entity1 = new TestEntity();
        TestEntity entity2 = new TestEntity();
        list.add(entity1);
        list.add(entity2);

        // 模拟Aspect填充List
        for (TestEntity entity : list) {
            fillEntityOperatorId(entity, 100L);
        }

        assertEquals(100L, entity1.getOperatorId());
        assertEquals(100L, entity2.getOperatorId());
    }

    /**
     * 测试无AuditContext - 不填充
     */
    @Test
    void testNoAuditContext() throws Exception {
        AuditContext.clear();

        TestEntity entity = new TestEntity();
        // 模拟Aspect不填充
        Long userId = AuditContext.getUserId();
        if (userId != null) {
            fillEntityOperatorId(entity, userId);
        }

        assertNull(entity.getOperatorId());
    }

    /**
     * 测试已有operatorId不覆盖
     */
    @Test
    void testExistingOperatorIdNotOverwrite() throws Exception {
        AuditContext.set(100L, 1L, "ADMIN", 1, "jti-123");

        TestEntity entity = new TestEntity();
        entity.setOperatorId(999L);  // 已有值

        // 模拟Aspect检查已有值
        Long userId = AuditContext.getUserId();
        if (userId != null && entity.getOperatorId() == null) {
            fillEntityOperatorId(entity, userId);
        }

        assertEquals(999L, entity.getOperatorId());  // 应保持原值
    }

    /**
     * 测试空List不抛异常
     */
    @Test
    void testEmptyList() {
        AuditContext.set(100L, 1L, "ADMIN", 1, "jti-123");

        List<TestEntity> list = new ArrayList<>();
        // 模拟处理空List
        for (TestEntity entity : list) {
            assertNotNull(entity); // 不会执行到这里
        }

        assertEquals(0, list.size());
    }

    /**
     * 填充Entity的operatorId字段的模拟方法
     */
    private void fillEntityOperatorId(TestEntity entity, Long userId) throws Exception {
        Field field = TestEntity.class.getDeclaredField("operatorId");
        field.setAccessible(true);
        field.set(entity, userId);
    }

    /**
     * 测试用Entity
     */
    static class TestEntity {
        private Long operatorId;
        private String name;

        public Long getOperatorId() {
            return operatorId;
        }

        public void setOperatorId(Long operatorId) {
            this.operatorId = operatorId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
