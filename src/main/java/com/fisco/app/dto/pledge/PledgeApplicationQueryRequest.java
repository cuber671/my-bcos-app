package com.fisco.app.dto.pledge;
import org.springframework.data.domain.Pageable;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * 质押申请查询请求DTO
 */
@Data
@ApiModel(value = "质押申请查询请求", description = "分页查询质押申请的条件")
public class PledgeApplicationQueryRequest {

    @ApiModelProperty(value = "仓单ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String receiptId;

    @ApiModelProperty(value = "货主企业ID", example = "owner-001")
    private String ownerId;

    @ApiModelProperty(value = "金融机构ID", example = "fin-001")
    private String financialInstitutionId;

    @ApiModelProperty(value = "申请状态", notes = "PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝, RELEASED-已释放")
    private String status;

    @ApiModelProperty(value = "页码（从0开始）", example = "0")
    private Integer page = 0;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "排序字段", example = "applyTime")
    private String sortField = "applyTime";

    @ApiModelProperty(value = "排序方向", notes = "ASC-升序, DESC-降序", example = "DESC")
    private String sortDirection = "DESC";

    /**
     * 转换为Pageable对象
     */
    public Pageable toPageable() {
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortField != null ? sortField : "applyTime"
        );
        return PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 10,
                sort
        );
    }
}
