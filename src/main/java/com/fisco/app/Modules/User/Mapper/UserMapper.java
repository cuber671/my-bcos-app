package com.fisco.app.Modules.User.Mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.Modules.User.Entity.User;

/**
 * 用户 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
