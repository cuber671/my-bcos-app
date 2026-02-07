-- 风险监测系统表
-- 创建时间: 2026-02-03

-- 风险评估表
CREATE TABLE risk_assessment (
    id VARCHAR(36) PRIMARY KEY,
    enterprise_address VARCHAR(42) NOT NULL COMMENT '企业地址',
    enterprise_name VARCHAR(200) COMMENT '企业名称',
    assessment_type VARCHAR(20) NOT NULL COMMENT '评估类型: CREDIT-信用, OVERDUE-逾期, COMPREHENSIVE-综合',
    assessment_time DATETIME(6) NOT NULL COMMENT '评估时间',
    risk_level VARCHAR(20) NOT NULL COMMENT '风险等级: VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH',
    risk_score INT COMMENT '风险评分(0-100)',
    credit_score INT COMMENT '信用评分',
    overdue_count INT COMMENT '逾期次数',
    overdue_amount BIGINT COMMENT '逾期金额(分)',
    overdue_rate DECIMAL(10,4) COMMENT '逾期率',
    total_liability BIGINT COMMENT '总负债(分)',
    transaction_count INT COMMENT '交易次数',
    warning_count INT COMMENT '风险预警数量',
    risk_factors TEXT COMMENT '风险因素权重分析(JSON)',
    recommendations TEXT COMMENT '改进建议(JSON)',
    created_at DATETIME(6) NOT NULL,
    INDEX idx_enterprise (enterprise_address),
    INDEX idx_assessment_time (assessment_time),
    INDEX idx_risk_level (risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险评估表';

-- 风险预警表
CREATE TABLE risk_alert (
    id VARCHAR(36) PRIMARY KEY,
    enterprise_address VARCHAR(42) NOT NULL COMMENT '企业地址',
    alert_type VARCHAR(50) NOT NULL COMMENT '预警类型: OVERDUE, CREDIT_LIMIT, BAD_DEBT, TRANSACTION',
    alert_level VARCHAR(20) NOT NULL COMMENT '预警等级: LOW, MEDIUM, HIGH, CRITICAL',
    alert_title VARCHAR(200) NOT NULL COMMENT '预警标题',
    alert_message TEXT NOT NULL COMMENT '预警消息',
    business_type VARCHAR(50) COMMENT '业务类型: BILL, RECEIVABLE, EWR',
    business_id VARCHAR(36) COMMENT '业务记录ID',
    threshold_value DECIMAL(20,2) COMMENT '阈值',
    current_value DECIMAL(20,2) COMMENT '当前值',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE-活跃, RESOLVED-已解决, IGNORED-已忽略',
    resolved_at DATETIME(6) COMMENT '解决时间',
    resolved_by VARCHAR(36) COMMENT '解决人ID',
    resolution_note TEXT COMMENT '解决说明',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_enterprise (enterprise_address),
    INDEX idx_alert_type (alert_type),
    INDEX idx_alert_level (alert_level),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险预警表';

-- 风险指标表
CREATE TABLE risk_metric (
    id VARCHAR(36) PRIMARY KEY,
    enterprise_address VARCHAR(42) NOT NULL COMMENT '企业地址',
    metric_name VARCHAR(50) NOT NULL COMMENT '指标名称',
    metric_value DECIMAL(20,4) NOT NULL COMMENT '指标值',
    metric_unit VARCHAR(20) COMMENT '指标单位',
    metric_date DATE NOT NULL COMMENT '指标日期',
    comparison_value DECIMAL(20,4) COMMENT '对比值(环比/同比)',
    comparison_type VARCHAR(20) COMMENT '对比类型: MOM-环比, YOY-同比',
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_enterprise_metric_date (enterprise_address, metric_name, metric_date),
    INDEX idx_enterprise (enterprise_address),
    INDEX idx_metric_name (metric_name),
    INDEX idx_metric_date (metric_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险指标表';

-- 风险报告表
CREATE TABLE risk_report (
    id VARCHAR(36) PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL COMMENT '报告类型: DAILY-日报, WEEKLY-周报, MONTHLY-月报, CUSTOM-自定义',
    report_period VARCHAR(50) NOT NULL COMMENT '报告周期',
    report_date DATE NOT NULL COMMENT '报告日期',
    summary TEXT COMMENT '报告摘要',
    total_risk_score INT COMMENT '总体风险评分',
    high_risk_enterprise_count INT COMMENT '高风险企业数量',
    medium_risk_enterprise_count INT COMMENT '中风险企业数量',
    low_risk_enterprise_count INT COMMENT '低风险企业数量',
    total_alert_count INT COMMENT '预警总数',
    critical_alert_count INT COMMENT '紧急预警数量',
    high_alert_count INT COMMENT '高级预警数量',
    report_data TEXT COMMENT '报告数据(JSON)',
    generated_by VARCHAR(36) COMMENT '生成人ID',
    created_at DATETIME(6) NOT NULL,
    INDEX idx_report_type (report_type),
    INDEX idx_report_date (report_date),
    INDEX idx_report_period (report_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险报告表';
