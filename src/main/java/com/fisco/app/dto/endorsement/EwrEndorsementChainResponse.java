package com.fisco.app.dto.endorsement;

import com.fisco.app.entity.warehouse.EwrEndorsementChain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 背书链响应DTO
 */
@Data
@ApiModel(value = "背书链响应", description = "背书链信息响应")
public class EwrEndorsementChainResponse {

    // ==================== 基础字段 ====================

    @ApiModelProperty(value = "背书ID", example = "b2c3d4e5-f6g7-8901-bcde-f23456789012")
    private String id;

    @ApiModelProperty(value = "仓单ID", example = "ewr-uuid-001")
    private String receiptId;

    @ApiModelProperty(value = "仓单编号", example = "EWR20260126000001")
    private String receiptNo;

    @ApiModelProperty(value = "背书编号", example = "END20260126000001")
    private String endorsementNo;

    // ==================== 背书企业信息 ====================

    @ApiModelProperty(value = "背书企业地址（转出方）", example = "0xabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
    private String endorseFrom;

    @ApiModelProperty(value = "背书企业名称", example = "XX贸易有限公司")
    private String endorseFromName;

    @ApiModelProperty(value = "被背书企业地址（转入方）", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String endorseTo;

    @ApiModelProperty(value = "被背书企业名称", example = "YY物流有限公司")
    private String endorseToName;

    // ==================== 经手人信息 ====================

    @ApiModelProperty(value = "转出方经手人ID", example = "user-uuid-001")
    private String operatorFromId;

    @ApiModelProperty(value = "转出方经手人姓名", example = "王五（转出企业员工）")
    private String operatorFromName;

    @ApiModelProperty(value = "转入方经手人ID", example = "user-uuid-002")
    private String operatorToId;

    @ApiModelProperty(value = "转入方经手人姓名", example = "赵六（转入企业员工）")
    private String operatorToName;

    // ==================== 背书类型和原因 ====================

    @ApiModelProperty(value = "背书类型", example = "TRANSFER")
    private String endorsementType;

    @ApiModelProperty(value = "背书类型描述", example = "转让")
    private String endorsementTypeDesc;

    @ApiModelProperty(value = "背书原因说明", example = "货物所有权转让")
    private String endorsementReason;

    // ==================== 货物信息快照 ====================

    @ApiModelProperty(value = "货物信息快照（JSON）", example = "{\"goods_name\":\"螺纹钢\",\"quantity\":1000}")
    private String goodsSnapshot;

    // ==================== 价格和金额 ====================

    @ApiModelProperty(value = "转让价格（元）", example = "4600.00")
    private BigDecimal transferPrice;

    @ApiModelProperty(value = "转让金额", example = "4600000.00")
    private BigDecimal transferAmount;

    // ==================== 区块链信息 ====================

    @ApiModelProperty(value = "背书交易哈希", example = "0xabcdef1234567890...")
    private String txHash;

    @ApiModelProperty(value = "区块高度", example = "12346")
    private Long blockNumber;

    @ApiModelProperty(value = "区块链时间戳", example = "2026-01-26T14:35:00")
    private LocalDateTime blockchainTimestamp;

    // ==================== 状态信息 ====================

    @ApiModelProperty(value = "背书状态", example = "CONFIRMED")
    private String endorsementStatus;

    @ApiModelProperty(value = "背书状态描述", example = "已确认")
    private String endorsementStatusDesc;

    @ApiModelProperty(value = "备注信息", example = "备注：背书协议已签署")
    private String remarks;

    // ==================== 时间戳 ====================

    @ApiModelProperty(value = "背书发起时间", example = "2026-01-26T14:30:00")
    private LocalDateTime endorsementTime;

    @ApiModelProperty(value = "确认时间", example = "2026-01-26T15:00:00")
    private LocalDateTime confirmedTime;

    @ApiModelProperty(value = "创建时间", example = "2026-01-26T14:30:00")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间", example = "2026-01-26T15:00:00")
    private LocalDateTime updatedAt;

    // ==================== 辅助方法 ====================

    /**
     * 从实体转换为响应DTO
     */
    public static EwrEndorsementChainResponse fromEntity(EwrEndorsementChain entity) {
        EwrEndorsementChainResponse response = new EwrEndorsementChainResponse();

        // 基础字段
        response.setId(entity.getId());
        response.setReceiptId(entity.getReceiptId());
        response.setReceiptNo(entity.getReceiptNo());
        response.setEndorsementNo(entity.getEndorsementNo());

        // 背书企业信息
        response.setEndorseFrom(entity.getEndorseFrom());
        response.setEndorseFromName(entity.getEndorseFromName());
        response.setEndorseTo(entity.getEndorseTo());
        response.setEndorseToName(entity.getEndorseToName());

        // 经手人信息
        response.setOperatorFromId(entity.getOperatorFromId());
        response.setOperatorFromName(entity.getOperatorFromName());
        response.setOperatorToId(entity.getOperatorToId());
        response.setOperatorToName(entity.getOperatorToName());

        // 背书类型和原因
        response.setEndorsementType(entity.getEndorsementType().name());
        response.setEndorsementTypeDesc(getEndorsementTypeDesc(entity.getEndorsementType()));
        response.setEndorsementReason(entity.getEndorsementReason());

        // 货物信息快照
        response.setGoodsSnapshot(entity.getGoodsSnapshot());

        // 价格和金额
        response.setTransferPrice(entity.getTransferPrice());
        response.setTransferAmount(entity.getTransferAmount());

        // 区块链信息
        response.setTxHash(entity.getTxHash());
        response.setBlockNumber(entity.getBlockNumber());
        response.setBlockchainTimestamp(entity.getBlockchainTimestamp());

        // 状态信息
        response.setEndorsementStatus(entity.getEndorsementStatus().name());
        response.setEndorsementStatusDesc(getEndorsementStatusDesc(entity.getEndorsementStatus()));
        response.setRemarks(entity.getRemarks());

        // 时间戳
        response.setEndorsementTime(entity.getEndorsementTime());
        response.setConfirmedTime(entity.getConfirmedTime());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }

    /**
     * 获取背书类型描述
     */
    private static String getEndorsementTypeDesc(EwrEndorsementChain.EndorsementType type) {
        switch (type) {
            case TRANSFER:
                return "转让";
            case PLEDGE:
                return "质押";
            case RELEASE:
                return "解押";
            case CANCEL:
                return "撤销";
            default:
                return "未知";
        }
    }

    /**
     * 获取背书状态描述
     */
    private static String getEndorsementStatusDesc(EwrEndorsementChain.EndorsementStatus status) {
        switch (status) {
            case PENDING:
                return "待确认";
            case CONFIRMED:
                return "已确认";
            case CANCELLED:
                return "已撤销";
            default:
                return "未知";
        }
    }
}
