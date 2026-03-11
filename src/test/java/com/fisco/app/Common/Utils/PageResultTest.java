package com.fisco.app.Common.Utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PageResult 分页响应类单元测试
 *
 * 测试覆盖：
 * - UT031-UT035: of()工厂方法、null处理
 */
class PageResultTest {

    /**
     * UT031: 测试标准分页创建
     */
    @Test
    void testPageResult_Standard() {
        List<String> list = Arrays.asList("item1", "item2", "item3");
        PageResult<String> result = PageResult.of(list, 100L, 1, 10);

        assertEquals(list, result.getList());
        assertNotNull(result.getPagination());
        assertEquals(100L, result.getPagination().getTotal());
        assertEquals(1, result.getPagination().getPage());
        assertEquals(10, result.getPagination().getPageSize());
        assertEquals(10, result.getPagination().getPages());
    }

    /**
     * UT032: 测试空列表
     */
    @Test
    void testPageResult_EmptyList() {
        List<String> emptyList = Collections.emptyList();
        PageResult<String> result = PageResult.of(emptyList, 0L, 1, 10);

        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getPagination().getTotal());
    }

    /**
     * UT033: 测试null列表
     */
    @Test
    void testPageResult_NullList() {
        PageResult<String> result = PageResult.of(null, 50L, 2, 20);

        assertNull(result.getList());
        assertNotNull(result.getPagination());
    }

    /**
     * UT034: 测试构造函数
     */
    @Test
    void testPageResult_Constructor() {
        List<String> list = Arrays.asList("a", "b");
        PageResult<String> result = new PageResult<>(list, 2L, 1, 2);

        assertEquals(list, result.getList());
        assertNotNull(result.getPagination());
    }

    /**
     * UT035: 测试无参构造函数
     */
    @Test
    void testPageResult_DefaultConstructor() {
        PageResult<String> result = new PageResult<>();

        assertNull(result.getList());
        assertNull(result.getPagination());
    }

    /**
     * UT036: 测试不同数据类型
     */
    @Test
    void testPageResult_DifferentTypes() {
        // Integer类型
        List<Integer> intList = Arrays.asList(1, 2, 3);
        PageResult<Integer> intResult = PageResult.of(intList, 3L, 1, 3);
        assertEquals(intList, intResult.getList());

        // 自定义对象类型
        List<StringBuilder> sbList = Arrays.asList(new StringBuilder("a"), new StringBuilder("b"));
        PageResult<StringBuilder> sbResult = PageResult.of(sbList, 2L, 1, 2);
        assertEquals(sbList, sbResult.getList());
    }
}
