-- 应收账款查询优化索引
-- 创建时间: 2026-02-09
-- 功能: 为应收账款常用查询添加复合索引，提升查询性能

-- 状态和到期日期复合索引（用于查询即将到期和已逾期的应收账款）
CREATE INDEX idx_status_due_date ON receivable(status, due_date) COMMENT '状态和到期日期复合索引';

-- 供应商和状态复合索引（用于查询供应商的应收账款状态分布）
CREATE INDEX idx_supplier_status ON receivable(supplier_address, status) COMMENT '供应商和状态复合索引';

-- 当前持有人和状态复合索引（用于查询持有人持有的应收账款状态分布）
CREATE INDEX idx_holder_status ON receivable(current_holder, status) COMMENT '持有人和状态复合索引';

-- 资金方和状态复合索引（用于查询资金方的融资应收账款状态分布）
CREATE INDEX idx_financier_status ON receivable(financier_address, status) COMMENT '资金方和状态复合索引';
