package com.fisco.app.Common.Utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 分页元数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pagination implements Serializable {

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码（从1开始）
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总页数
     */
    private Integer pages;

    /**
     * 便捷构造函数
     */
    public Pagination(Long total, Integer page, Integer pageSize) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        // 计算总页数
        if (total != null && pageSize != null && pageSize > 0) {
            this.pages = (int) Math.ceil((double) total / pageSize);
        } else {
            this.pages = 0;
        }
    }
}
