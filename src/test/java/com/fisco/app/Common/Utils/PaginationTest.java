package com.fisco.app.Common.Utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pagination 分页元数据类单元测试
 *
 * 测试覆盖：
 * - UT019-UT022: 构造函数、分页计算
 * - UT023-UT028: 边界测试(0, null)
 */
class PaginationTest {

    /**
     * UT019: 测试标准分页计算
     * 总数100，每页10，第1页 -> 总页数10
     */
    @Test
    void testPagination_Calculation() {
        Pagination pagination = new Pagination(100L, 1, 10);

        assertEquals(100L, pagination.getTotal());
        assertEquals(1, pagination.getPage());
        assertEquals(10, pagination.getPageSize());
        assertEquals(10, pagination.getPages());
    }

    /**
     * UT020: 测试第2页
     * 总数100，每页10，第2页 -> 总页数10
     */
    @Test
    void testPagination_Page2() {
        Pagination pagination = new Pagination(100L, 2, 10);

        assertEquals(2, pagination.getPage());
        assertEquals(10, pagination.getPages());
    }

    /**
     * UT021: 测试最后一页
     * 总数95，每页10，第10页 -> 总页数10
     */
    @Test
    void testPagination_LastPage() {
        Pagination pagination = new Pagination(95L, 10, 10);

        assertEquals(10, pagination.getPage());
        assertEquals(10, pagination.getPages());
    }

    /**
     * UT022: 测试不能整除的情况
     * 总数95，每页10 -> 总页数10 (ceil(95/10)=10)
     */
    @Test
    void testPagination_NotDivisible() {
        Pagination pagination = new Pagination(95L, 1, 10);

        assertEquals(10, pagination.getPages());
    }

    // ==================== 边界测试 ====================

    /**
     * UT023: 测试总数为0
     * 总数0，每页10 -> 总页数0
     */
    @Test
    void testPagination_TotalZero() {
        Pagination pagination = new Pagination(0L, 1, 10);

        assertEquals(0L, pagination.getTotal());
        assertEquals(0, pagination.getPages());
    }

    /**
     * UT024: 测试总数为null
     */
    @Test
    void testPagination_TotalNull() {
        Pagination pagination = new Pagination(null, 1, 10);

        assertNull(pagination.getTotal());
        assertEquals(0, pagination.getPages());
    }

    /**
     * UT025: 测试pageSize为0
     */
    @Test
    void testPagination_PageSizeZero() {
        Pagination pagination = new Pagination(100L, 1, 0);

        assertEquals(0, pagination.getPageSize());
        assertEquals(0, pagination.getPages());
    }

    /**
     * UT026: 测试pageSize为null
     */
    @Test
    void testPagination_PageSizeNull() {
        Pagination pagination = new Pagination(100L, 1, null);

        assertNull(pagination.getPageSize());
        assertEquals(0, pagination.getPages());
    }

    /**
     * UT027: 测试无参构造函数
     */
    @Test
    void testPagination_DefaultConstructor() {
        Pagination pagination = new Pagination();

        assertNull(pagination.getTotal());
        assertNull(pagination.getPage());
        assertNull(pagination.getPageSize());
        assertNull(pagination.getPages());
    }

    /**
     * UT028: 测试全参构造函数
     */
    @Test
    void testPagination_AllArgsConstructor() {
        Pagination pagination = new Pagination(200L, 3, 20, 10);

        assertEquals(200L, pagination.getTotal());
        assertEquals(3, pagination.getPage());
        assertEquals(20, pagination.getPageSize());
        assertEquals(10, pagination.getPages());
    }

    /**
     * UT029: 测试页码为0
     */
    @Test
    void testPagination_PageZero() {
        Pagination pagination = new Pagination(100L, 0, 10);

        assertEquals(0, pagination.getPage());
    }

    /**
     * UT030: 测试总数小于pageSize
     * 总数5，每页10 -> 总页数1
     */
    @Test
    void testPagination_LessThanPageSize() {
        Pagination pagination = new Pagination(5L, 1, 10);

        assertEquals(1, pagination.getPages());
    }
}
