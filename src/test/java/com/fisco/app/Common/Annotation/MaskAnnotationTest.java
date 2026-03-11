package com.fisco.app.Common.Annotation;

import com.fisco.app.Common.Utils.DataMaskingUtil.MaskType;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Mask 注解单元测试
 *
 * 测试覆盖：
 * - UT031: 注解默认参数
 * - UT032: 自定义注解参数
 * - UT033: @Target和@Retention验证
 * - UT034: 注解在字段上使用
 */
class MaskAnnotationTest {

    /**
     * UT031: 测试注解默认参数
     */
    @Test
    void testDefaultAnnotationParameters() throws NoSuchFieldException {
        Field field = TestClassWithDefaultMask.class.getDeclaredField("phone");
        Mask annotation = field.getAnnotation(Mask.class);

        assertNotNull(annotation);
        assertEquals(MaskType.PHONE, annotation.value());
    }

    /**
     * UT032: 测试自定义注解参数
     */
    @Test
    void testCustomAnnotationParameters() throws NoSuchFieldException {
        // 测试ID_CARD类型
        Field idCardField = TestClassWithCustomMask.class.getDeclaredField("idCard");
        Mask idCardAnnotation = idCardField.getAnnotation(Mask.class);
        assertNotNull(idCardAnnotation);
        assertEquals(MaskType.ID_CARD, idCardAnnotation.value());

        // 测试WALLET_ADDRESS类型
        Field walletField = TestClassWithCustomMask.class.getDeclaredField("walletAddress");
        Mask walletAnnotation = walletField.getAnnotation(Mask.class);
        assertNotNull(walletAnnotation);
        assertEquals(MaskType.WALLET_ADDRESS, walletAnnotation.value());
    }

    /**
     * UT033: 测试注解的@Target和@Retention
     */
    @Test
    void testAnnotationRetentionAndTarget() {
        Mask annotation = getDefaultAnnotation();

        // 验证@Target - 字段级别
        java.lang.annotation.Target target = annotation.annotationType().getAnnotation(java.lang.annotation.Target.class);
        assertNotNull(target);
        assertArrayEquals(
            new java.lang.annotation.ElementType[]{
                java.lang.annotation.ElementType.FIELD
            },
            target.value()
        );

        // 验证@Retention - 运行时保留
        Retention retention = annotation.annotationType().getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    /**
     * UT034: 测试注解在实体类字段上使用
     */
    @Test
    void testAnnotationOnField() throws NoSuchFieldException {
        // 验证phone字段有@Mask注解
        Field phoneField = TestEntityWithMask.class.getDeclaredField("phoneNumber");
        assertTrue(phoneField.isAnnotationPresent(Mask.class));

        // 验证idCard字段有@Mask注解
        Field idCardField = TestEntityWithMask.class.getDeclaredField("idCardNumber");
        assertTrue(idCardField.isAnnotationPresent(Mask.class));

        // 验证email字段没有@Mask注解
        Field emailField = TestEntityWithMask.class.getDeclaredField("email");
        assertFalse(emailField.isAnnotationPresent(Mask.class));
    }

    // ==================== 辅助方法 ====================

    private Mask getDefaultAnnotation() {
        try {
            TestClassWithDefaultMask testClass = new TestClassWithDefaultMask();
            return testClass.getClass().getDeclaredField("phone").getAnnotation(Mask.class);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== 测试用类 ====================

    static class TestClassWithDefaultMask {
        @Mask
        public String phone;
    }

    static class TestClassWithCustomMask {
        @Mask(MaskType.ID_CARD)
        public String idCard;

        @Mask(MaskType.WALLET_ADDRESS)
        public String walletAddress;
    }

    /**
     * 测试实体类 - 模拟实际使用场景
     */
    static class TestEntityWithMask {
        @Mask(MaskType.PHONE)
        private String phoneNumber;

        @Mask(MaskType.ID_CARD)
        private String idCardNumber;

        @Mask(MaskType.WALLET_ADDRESS)
        private String walletAddress;

        private String email;  // 不需要脱敏

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getIdCardNumber() { return idCardNumber; }
        public void setIdCardNumber(String idCardNumber) { this.idCardNumber = idCardNumber; }
        public String getWalletAddress() { return walletAddress; }
        public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
