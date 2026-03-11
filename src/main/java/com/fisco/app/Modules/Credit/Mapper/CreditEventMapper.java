package com.fisco.app.Modules.Credit.Mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.Modules.Credit.Entity.CreditEvent;

/**
 * 信用事件 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface CreditEventMapper extends BaseMapper<CreditEvent> {
}
