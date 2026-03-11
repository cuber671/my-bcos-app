package com.fisco.app.Common.Utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应包装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 分页元数据
     */
    private Pagination pagination;

    /**
     * 便捷构造函数
     */
    public PageResult(List<T> list, Long total, Integer page, Integer pageSize) {
        this.list = list;
        this.pagination = new Pagination(total, page, pageSize);
    }

    /**
     * 创建分页结果（从MyBatis-Plus的IPage转换）
     */
    public static <T> PageResult<T> of(List<T> list, Long total, Integer page, Integer pageSize) {
        return new PageResult<>(list, total, page, pageSize);
    }
}
