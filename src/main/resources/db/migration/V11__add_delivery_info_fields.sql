-- ============================================================
-- 电子仓单表 - 添加提货信息字段
-- 版本: V11
-- 创建时间: 2026-01-27
-- 说明: 添加实际提货时的详细信息字段
-- ============================================================

-- 添加提货信息相关字段
ALTER TABLE electronic_warehouse_receipt
ADD COLUMN delivery_person_name VARCHAR(100) COMMENT '提货人姓名|实际提取货物的人员姓名',
ADD COLUMN delivery_person_contact VARCHAR(50) COMMENT '提货人联系方式|手机号或电话',
ADD COLUMN delivery_no VARCHAR(64) COMMENT '提货单号|提货凭证编号',
ADD COLUMN vehicle_plate VARCHAR(20) COMMENT '运输车牌号|提货车辆车牌号',
ADD COLUMN driver_name VARCHAR(100) COMMENT '司机姓名|驾驶员姓名';

-- 添加索引以支持按提货信息查询
ALTER TABLE electronic_warehouse_receipt
ADD INDEX idx_delivery_no (delivery_no) COMMENT '按提货单号查询',
ADD INDEX idx_delivery_date (actual_delivery_date) COMMENT '按提货日期查询';

-- ============================================================
-- 字段说明
-- ============================================================
-- delivery_person_name: 记录实际来仓库提货的人员姓名
-- delivery_person_contact: 提货人的联系方式，便于后续联系
-- delivery_no: 提货单号或提货凭证编号，用于财务对账
-- vehicle_plate: 运货车辆的车牌号，便于出入管理
-- driver_name: 司机姓名，用于车辆和司机管理
