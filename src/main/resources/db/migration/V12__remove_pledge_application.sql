-- =====================================================
-- Migration V12: Remove PledgeApplication dependency
-- =====================================================
-- This migration adds endorsement_id and endorsement_no columns
-- to ewr_pledge_record and ewr_financing_record tables to link
-- directly to EwrEndorsementChain instead of PledgeApplication.
--
-- The ewr_pledge_application table is kept for historical data
-- compatibility but marked as deprecated.
-- =====================================================

-- Add endorsement_id and endorsement_no columns to ewr_pledge_record
ALTER TABLE ewr_pledge_record
ADD COLUMN endorsement_id VARCHAR(36) COMMENT '背书ID（链接到ewr_endorsement_chain）',
ADD COLUMN endorsement_no VARCHAR(64) COMMENT '背书编号（冗余）';

-- Add endorsement_id and endorsement_no columns to ewr_financing_record
ALTER TABLE ewr_financing_record
ADD COLUMN endorsement_id VARCHAR(36) COMMENT '背书ID（链接到ewr_endorsement_chain）',
ADD COLUMN endorsement_no VARCHAR(64) COMMENT '背书编号（冗余）';

-- Create indexes on endorsement_id for both tables
CREATE INDEX idx_endorsement_id ON ewr_pledge_record(endorsement_id);
CREATE INDEX idx_endorsement_id ON ewr_financing_record(endorsement_id);

-- Mark ewr_pledge_application table as deprecated (add a comment)
-- Note: This table is kept for historical data compatibility
ALTER TABLE ewr_pledge_application COMMENT = '【已废弃】质押申请表 - 已改用ewr_endorsement_chain直接管理质押流程，此表仅保留用于历史数据查询';
