-- ============================================
-- 外键约束脚本
-- 说明：添加表之间的外键约束，保证数据引用完整性
-- 注意：外键约束会影响写入性能，建议在低峰期执行
-- ============================================

USE `supply_chain_finance`;

-- ============================================
-- 外键说明
-- ============================================
-- 外键命名规则：fk_{子表名}_{父表名}
-- 约束规则：
--   ON DELETE RESTRICT  - 防止删除被引用的记录
--   ON UPDATE CASCADE   - 父表ID更新时自动同步到子表
-- ============================================

-- ============================================
-- 用户模块外键
-- ============================================

-- t_user表的外键
ALTER TABLE `user`
  ADD CONSTRAINT `fk_t_user_enterprise`
    FOREIGN KEY (`enterprise_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- t_user_activity表的外键
ALTER TABLE `user_activity`
  ADD CONSTRAINT `fk_t_user_activity_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `user`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_user_permission表的外键
ALTER TABLE `user_permission`
  ADD CONSTRAINT `fk_t_user_permission_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `user`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_invitation_code表的外键
ALTER TABLE `invitation_code`
  ADD CONSTRAINT `fk_t_invitation_code_enterprise`
    FOREIGN KEY (`enterprise_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- ============================================
-- 企业模块外键
-- ============================================

-- t_credit_rating_history表的外键
ALTER TABLE `credit_rating_history`
  ADD CONSTRAINT `fk_t_credit_rating_history_enterprise`
    FOREIGN KEY (`enterprise_address`)
    REFERENCES `enterprise`(`address`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- t_enterprise_audit_log表的外键
ALTER TABLE `enterprise_audit_log`
  ADD CONSTRAINT `fk_t_enterprise_audit_log_enterprise`
    FOREIGN KEY (`enterprise_address`)
    REFERENCES `enterprise`(`address`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- ============================================
-- 票据模块外键
-- ============================================

-- t_bill表的外键
ALTER TABLE `bill`
  ADD CONSTRAINT `fk_t_bill_drawer`
    FOREIGN KEY (`drawer_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_bill_drawee`
    FOREIGN KEY (`drawee_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_bill_payee`
    FOREIGN KEY (`payee_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_bill_current_holder`
    FOREIGN KEY (`current_holder_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_bill_parent`
    FOREIGN KEY (`parent_bill_id`)
    REFERENCES `bill`(`bill_id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- t_bill_discount表的外键
ALTER TABLE `bill_discount`
  ADD CONSTRAINT `fk_t_bill_discount_bill`
    FOREIGN KEY (`bill_id`)
    REFERENCES `bill`(`bill_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_bill_discount_institution`
    FOREIGN KEY (`discount_institution_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_bill_discount_applicant`
    FOREIGN KEY (`applicant_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- t_bill_endorsement表的外键
ALTER TABLE `bill_endorsement`
  ADD CONSTRAINT `fk_t_bill_endorsement_bill`
    FOREIGN KEY (`bill_id`)
    REFERENCES `bill`(`bill_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_bill_endorsement_endorser`
    FOREIGN KEY (`endorser_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_bill_endorsement_endorsee`
    FOREIGN KEY (`endorsee_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- t_bill_finance_application表的外键
ALTER TABLE `bill_finance_application`
  ADD CONSTRAINT `fk_t_bill_finance_application_bill`
    FOREIGN KEY (`bill_id`)
    REFERENCES `bill`(`bill_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_bill_finance_application_applicant`
    FOREIGN KEY (`applicant_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_bill_finance_application_financial`
    FOREIGN KEY (`financial_institution_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- t_bill_guarantee表的外键
ALTER TABLE `bill_guarantee`
  ADD CONSTRAINT `fk_t_bill_guarantee_bill`
    FOREIGN KEY (`bill_id`)
    REFERENCES `bill`(`bill_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_bill_guarantee_guarantor`
    FOREIGN KEY (`guarantor_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- t_bill_investment表的外键
ALTER TABLE `bill_investment`
  ADD CONSTRAINT `fk_t_bill_investment_bill`
    FOREIGN KEY (`bill_id`)
    REFERENCES `bill`(`bill_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_bill_investment_investor`
    FOREIGN KEY (`investor_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- t_bill_pledge_application表的外键
ALTER TABLE `bill_pledge_application`
  ADD CONSTRAINT `fk_t_bill_pledge_application_bill`
    FOREIGN KEY (`bill_id`)
    REFERENCES `bill`(`bill_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_bill_pledge_application_applicant`
    FOREIGN KEY (`applicant_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_bill_pledge_application_financial`
    FOREIGN KEY (`financial_institution_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- t_bill_split_application表的外键
ALTER TABLE `bill_split_application`
  ADD CONSTRAINT `fk_t_bill_split_application_parent`
    FOREIGN KEY (`parent_bill_id`)
    REFERENCES `bill`(`bill_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_bill_merge_application表的外键（需要在t_bill_split_application之后执行）
-- ALTER TABLE `bill_merge_application`
--   ADD CONSTRAINT `fk_t_bill_merge_application_merged`
--     FOREIGN KEY (`merged_bill_id`)
--     REFERENCES `bill`(`bill_id`)
--     ON DELETE SET NULL
--     ON UPDATE CASCADE;

-- t_discount_record表的外键
ALTER TABLE `discount_record`
  ADD CONSTRAINT `fk_t_discount_record_bill`
    FOREIGN KEY (`bill_id`)
    REFERENCES `bill`(`bill_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_endorsement表的外键
ALTER TABLE `endorsement`
  ADD CONSTRAINT `fk_t_endorsement_bill`
    FOREIGN KEY (`bill_id`)
    REFERENCES `bill`(`bill_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_repayment_record表的外键
ALTER TABLE `repayment_record`
  ADD CONSTRAINT `fk_t_repayment_record_bill`
    FOREIGN KEY (`bill_id`)
    REFERENCES `bill`(`bill_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_bill_recourse表的外键
ALTER TABLE `bill_recourse`
  ADD CONSTRAINT `fk_t_bill_recourse_bill`
    FOREIGN KEY (`bill_id`)
    REFERENCES `bill`(`bill_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_bill_settlement表的外键
ALTER TABLE `bill_settlement`
  ADD CONSTRAINT `fk_t_bill_settlement_bill`
    FOREIGN KEY (`bill_id`)
    REFERENCES `bill`(`bill_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- ============================================
-- 仓单模块外键
-- ============================================

-- t_electronic_warehouse_receipt表的外键
ALTER TABLE `electronic_warehouse_receipt`
  ADD CONSTRAINT `fk_t_ewr_warehouse`
    FOREIGN KEY (`warehouse_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_ewr_owner`
    FOREIGN KEY (`owner_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_ewr_parent`
    FOREIGN KEY (`parent_receipt_id`)
    REFERENCES `electronic_warehouse_receipt`(`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE;

-- t_receipt_split_application表的外键
ALTER TABLE `receipt_split_application`
  ADD CONSTRAINT `fk_t_receipt_split_parent`
    FOREIGN KEY (`parent_receipt_id`)
    REFERENCES `electronic_warehouse_receipt`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_receipt_merge_application表的外键
ALTER TABLE `receipt_merge_application`
  ADD CONSTRAINT `fk_t_receipt_merge_merged`
    FOREIGN KEY (`merged_receipt_id`)
    REFERENCES `electronic_warehouse_receipt`(`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE;

-- t_warehouse_receipt_pledge表的外键
ALTER TABLE `warehouse_receipt_pledge`
  ADD CONSTRAINT `fk_t_warehouse_receipt_pledge_receipt`
    FOREIGN KEY (`receipt_id`)
    REFERENCES `electronic_warehouse_receipt`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_warehouse_receipt_pledge_owner`
    FOREIGN KEY (`owner_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_warehouse_receipt_pledge_financial`
    FOREIGN KEY (`financial_institution_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- ============================================
-- 应收账款模块外键
-- ============================================

-- t_receivable表的外键
ALTER TABLE `receivable`
  ADD CONSTRAINT `fk_t_receivable_supplier`
    FOREIGN KEY (`supplier_address`)
    REFERENCES `enterprise`(`address`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_receivable_core`
    FOREIGN KEY (`core_enterprise_address`)
    REFERENCES `enterprise`(`address`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_receivable_holder`
    FOREIGN KEY (`current_holder`)
    REFERENCES `enterprise`(`address`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_receivable_financier`
    FOREIGN KEY (`financier_address`)
    REFERENCES `enterprise`(`address`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_receivable_parent`
    FOREIGN KEY (`parent_receivable_id`)
    REFERENCES `receivable`(`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE;

-- t_receivable_repayment_record表的外键
ALTER TABLE `receivable_repayment_record`
  ADD CONSTRAINT `fk_t_receivable_repayment_receivable`
    FOREIGN KEY (`receivable_id`)
    REFERENCES `receivable`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_receivable_transfer表的外键
ALTER TABLE `receivable_transfer`
  ADD CONSTRAINT `fk_t_receivable_transfer_receivable`
    FOREIGN KEY (`receivable_id`)
    REFERENCES `receivable`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- ============================================
-- 质押模块外键
-- ============================================

-- t_ewr_pledge_record表的外键
ALTER TABLE `ewr_pledge_record`
  ADD CONSTRAINT `fk_t_ewr_pledge_receipt`
    FOREIGN KEY (`receipt_id`)
    REFERENCES `electronic_warehouse_receipt`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_ewr_pledge_owner`
    FOREIGN KEY (`owner_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_ewr_pledge_financial`
    FOREIGN KEY (`financial_institution_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- t_ewr_pledge_application表的外键
ALTER TABLE `ewr_pledge_application`
  ADD CONSTRAINT `fk_t_ewr_pledge_app_receipt`
    FOREIGN KEY (`receipt_id`)
    REFERENCES `electronic_warehouse_receipt`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_ewr_pledge_app_owner`
    FOREIGN KEY (`owner_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_ewr_pledge_app_financial`
    FOREIGN KEY (`financial_institution_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- t_release_record表的外键
ALTER TABLE `release_record`
  ADD CONSTRAINT `fk_t_release_receipt`
    FOREIGN KEY (`receipt_id`)
    REFERENCES `electronic_warehouse_receipt`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- ============================================
-- 信用额度模块外键
-- ============================================

-- t_credit_limit表的外键
ALTER TABLE `credit_limit`
  ADD CONSTRAINT `fk_t_credit_limit_enterprise`
    FOREIGN KEY (`enterprise_address`)
    REFERENCES `enterprise`(`address`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_credit_limit_usage表的外键
ALTER TABLE `credit_limit_usage`
  ADD CONSTRAINT `fk_t_credit_limit_usage_credit_limit`
    FOREIGN KEY (`credit_limit_id`)
    REFERENCES `credit_limit`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_credit_limit_adjust_request表的外键
ALTER TABLE `credit_limit_adjust_request`
  ADD CONSTRAINT `fk_t_credit_limit_adjust_credit_limit`
    FOREIGN KEY (`credit_limit_id`)
    REFERENCES `credit_limit`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_credit_limit_warning表的外键
ALTER TABLE `credit_limit_warning`
  ADD CONSTRAINT `fk_t_credit_limit_warning_credit_limit`
    FOREIGN KEY (`credit_limit_id`)
    REFERENCES `credit_limit`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- ============================================
-- 风险管理模块外键
-- ============================================

-- t_risk_assessment表的外键
ALTER TABLE `risk_assessment`
  ADD CONSTRAINT `fk_t_risk_assessment_enterprise`
    FOREIGN KEY (`enterprise_address`)
    REFERENCES `enterprise`(`address`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_bad_debt_record表的外键
ALTER TABLE `bad_debt_record`
  ADD CONSTRAINT `fk_t_bad_debt_receivable`
    FOREIGN KEY (`receivable_id`)
    REFERENCES `receivable`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_ewr_financing_record表的外键
ALTER TABLE `ewr_financing_record`
  ADD CONSTRAINT `fk_t_ewr_financing_receipt`
    FOREIGN KEY (`receipt_id`)
    REFERENCES `electronic_warehouse_receipt`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_ewr_financing_owner`
    FOREIGN KEY (`owner_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_t_ewr_financing_financial`
    FOREIGN KEY (`financial_institution_id`)
    REFERENCES `enterprise`(`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- t_overdue_penalty_record表的外键
ALTER TABLE `overdue_penalty_record`
  ADD CONSTRAINT `fk_t_overdue_penalty_receivable`
    FOREIGN KEY (`receivable_id`)
    REFERENCES `receivable`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- t_overdue_remind_record表的外键
ALTER TABLE `overdue_remind_record`
  ADD CONSTRAINT `fk_t_overdue_remind_receivable`
    FOREIGN KEY (`receivable_id`)
    REFERENCES `receivable`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- ============================================
-- 外键约束说明
-- ============================================
--
-- 外键约束的优点：
--   1. 保证数据引用完整性，防止出现孤儿记录
--   2. 自动级联更新，减少手动维护工作
--   3. 数据库层面强制约束，提高数据质量
--
-- 外键约束的缺点：
--   1. 影响写入性能（每次插入/更新需要检查外键）
--   2. 增加数据库锁竞争
--   3. 灵活性降低，某些特殊场景需要临时禁用外键
--
-- 性能优化建议：
--   1. 为外键字段添加索引（已在各表DDL中添加）
--   2. 在低峰期执行大批量数据操作
--   3. 考虑在应用层维护关联关系，不使用外键约束
--
-- 外键检查命令：
--   查看外键状态：SELECT * FROM information_schema.KEY_COLUMN_USAGE
--                   WHERE TABLE_SCHEMA = 'supply_chain_finance'
--                   AND REFERENCED_TABLE_NAME IS NOT NULL;
--
--   禁用外键检查：SET FOREIGN_KEY_CHECKS = 0;
--   启用外键检查：SET FOREIGN_KEY_CHECKS = 1;
--
-- ============================================
