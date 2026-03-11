package com.fisco.app.Common.Annotation.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HexAddressValidator 单元测试
 */
class HexAddressValidatorTest {

    private HexAddressValidator validator;

    @BeforeEach
    void setUp() {
        validator = new HexAddressValidator();
    }

    // ==================== 有效地址测试 ====================

    @Test
    void testValidAddress() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertTrue(validator.isValid("0x1234567890abcdef1234567890abcdef12345678", null));
    }

    @Test
    void testValidAddressAllLowercase() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertTrue(validator.isValid("0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", null));
    }

    @Test
    void testValidAddressAllUppercase() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertTrue(validator.isValid("0xAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", null));
    }

    @Test
    void testMixedCaseHex() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertTrue(validator.isValid("0xABCDEF1234567890abcdef1234567890AB", null));
    }

    @Test
    void testUpperCasePrefix() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertTrue(validator.isValid("0XABCDEF1234567890ABCDEF1234567890AB", null));
    }

    // ==================== 无效地址测试 ====================

    @Test
    void testInvalidAddressNoPrefix() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertFalse(validator.isValid("1234567890abcdef1234567890abcdef12345678", null));
    }

    @Test
    void testInvalidAddressTooShort() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertFalse(validator.isValid("0x1234567890abcdef1234567890abcdef", null));
    }

    @Test
    void testInvalidAddressTooShort39() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertFalse(validator.isValid("0x1234567890abcdef1234567890abcdef1234567", null));
    }

    @Test
    void testInvalidAddressTooLong() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertFalse(validator.isValid("0x1234567890abcdef1234567890abcdef1234567890", null));
    }

    @Test
    void testInvalidAddressTooLong41() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertFalse(validator.isValid("0x1234567890abcdef1234567890abcdef123456789", null));
    }

    @Test
    void testInvalidAddressNonHex() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertFalse(validator.isValid("0x1234567890abcdef1234567890abcdef1234567g", null));
    }

    @Test
    void testInvalidAddressNonHex2() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertFalse(validator.isValid("0x1234567890abcdef1234567890abcdef1234567z", null));
    }

    @Test
    void testInvalidAddressSpecialChars() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertFalse(validator.isValid("0x1234567890abcdef1234567890abcdef1234567!", null));
    }

    // ==================== 空值测试 ====================

    @Test
    void testNullAllowNullTrue() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void testNullAllowNullFalse() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, false);
        assertFalse(validator.isValid(null, null));
    }

    @Test
    void testEmptyAllowNullTrue() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertTrue(validator.isValid("", null));
    }

    @Test
    void testEmptyAllowNullFalse() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, false);
        assertFalse(validator.isValid("", null));
    }

    // ==================== 边界测试 ====================

    @Test
    void testExactly40HexChars() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertTrue(validator.isValid("0x" + "a".repeat(40), null));
    }

    @Test
    void testExactly39HexChars() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertFalse(validator.isValid("0x" + "a".repeat(39), null));
    }

    @Test
    void testExactly41HexChars() throws Exception {
        Field field = HexAddressValidator.class.getDeclaredField("allowNull");
        field.setAccessible(true);
        field.set(validator, true);
        assertFalse(validator.isValid("0x" + "a".repeat(41), null));
    }
}
