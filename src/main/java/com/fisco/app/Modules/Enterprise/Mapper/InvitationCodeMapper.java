package com.fisco.app.Modules.Enterprise.Mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.Modules.Enterprise.Entity.InvitationCode;

/**
 * 邀请码 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface InvitationCodeMapper extends BaseMapper<InvitationCode> {
}
