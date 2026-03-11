package com.fisco.app.Modules.Warehouse.Mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.Modules.Warehouse.Entity.StockOrder;

/**
 * 入库单 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface StockOrderMapper extends BaseMapper<StockOrder> {
}
