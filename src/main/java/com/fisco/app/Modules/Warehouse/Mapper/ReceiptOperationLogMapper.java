package com.fisco.app.Modules.Warehouse.Mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.Modules.Warehouse.Entity.ReceiptOperationLog;

/**
 * 仓单操作记录 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface ReceiptOperationLogMapper extends BaseMapper<ReceiptOperationLog> {
}
