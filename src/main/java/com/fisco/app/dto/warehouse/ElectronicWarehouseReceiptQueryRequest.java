package com.fisco.app.dto.warehouse;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 电子仓单查询请求DTO
 */
@Data
@ApiModel(value = "电子仓单查询请求", description = "电子仓单查询参数")
public class ElectronicWarehouseReceiptQueryRequest {

    @ApiModelProperty(value = "页码", example = "0")
    private Integer page = 0;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "仓单编号", example = "EWR20260126000001")
    private String receiptNo;

    @ApiModelProperty(value = "仓储企业ID", example = "warehouse-uuid-001")
    private String warehouseId;

    @ApiModelProperty(value = "仓储方地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String warehouseAddress;

    @ApiModelProperty(value = "货主企业ID", example = "owner-uuid-001")
    private String ownerId;

    @ApiModelProperty(value = "货主地址", example = "0xabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
    private String ownerAddress;

    @ApiModelProperty(value = "持单人地址", example = "0xabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
    private String holderAddress;

    @ApiModelProperty(value = "仓单状态", example = "NORMAL", notes = "DRAFT-草稿, NORMAL-正常, PLEDGED-已质押, TRANSFERRED-已转让, FROZEN-已冻结, EXPIRED-已过期, DELIVERED-已提货, CANCELLED-已取消")
    private String receiptStatus;

    @ApiModelProperty(value = "货物名称", example = "螺纹钢")
    private String goodsName;

    @ApiModelProperty(value = "入库时间开始", example = "2026-01-01T00:00:00")
    private String storageDateStart;

    @ApiModelProperty(value = "入库时间结束", example = "2026-01-31T23:59:59")
    private String storageDateEnd;

    @ApiModelProperty(value = "有效期开始", example = "2026-01-01T00:00:00")
    private String expiryDateStart;

    @ApiModelProperty(value = "有效期结束", example = "2026-12-31T23:59:59")
    private String expiryDateEnd;

    @ApiModelProperty(value = "是否已融资", example = "false")
    private Boolean isFinanced;

    @ApiModelProperty(value = "区块链上链状态", example = "SYNCED", notes = "PENDING-待上链, SYNCED-已同步, FAILED-上链失败, VERIFIED-已验证")
    private String blockchainStatus;

    @ApiModelProperty(value = "排序字段", example = "storage_date")
    private String sortField = "createdAt";

    @ApiModelProperty(value = "排序方向", example = "DESC", notes = "ASC-升序, DESC-降序")
    private String sortOrder = "DESC";
}
