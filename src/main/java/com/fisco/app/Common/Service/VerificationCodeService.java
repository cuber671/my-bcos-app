package com.fisco.app.Common.Service;

import java.util.Map;

/**
 * 验证码服务接口
 * 用于处理敏感操作的二次校验
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface VerificationCodeService {

    /**
     * 发送验证码
     * 生成验证码并发送到指定目标（短信/邮箱）
     *
     * @param entId   企业ID
     * @param type   验证码类型
     * @param target 接收目标（手机号/邮箱）
     * @return 发送结果
     */
    Map<String, Object> sendCode(String entId, String type, String target);

    /**
     * 校验验证码
     * 验证用户提交的验证码是否正确且未过期
     *
     * @param entId  企业ID
     * @param type  验证码类型
     * @param code 用户提交的验证码
     * @return 校验结果
     */
    boolean verifyCode(String entId, String type, String code);

    /**
     * 创建验证码
     * 仅创建验证码，不发送（用于测试或自定义发送逻辑）
     *
     * @param entId 企业ID
     * @param type  验证码类型
     * @return 生成的验证码
     */
    String createCode(String entId, String type);
}
