package com.fisco.app.Modules.Warehouse.Mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.Modules.Warehouse.Entity.WarehouseReceipt;

/**
 * 电子仓单 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface WarehouseReceiptMapper extends BaseMapper<WarehouseReceipt> {
}
