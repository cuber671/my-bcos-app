package com.fisco.app.Modules.Auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.Common.Service.TokenService;
import com.fisco.app.Common.Utils.Result;

import lombok.extern.slf4j.Slf4j;

/**
 * 登出控制器
 * 提供Token吊销功能，将JWT加入黑名单
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class LogoutController {

    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private TokenService tokenService;

    /**
     * 登出接口
     * 将当前Token加入黑名单，强制其失效
     *
     * @param authHeader Authorization请求头
     * @return 操作结果
     */
    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("登出失败：未提供有效的Authorization头");
            return Result.error(401, "Invalid authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            boolean revoked = tokenService.revokeToken(token);
            if (revoked) {
                log.info("Token吊销成功");
                return Result.success("Logout successful");
            } else {
                // Token无效或已过期，也返回成功，避免泄露Token状态信息
                log.info("Token无效或已过期，视为登出成功");
                return Result.success("Logout successful");
            }
        } catch (Exception e) {
            // 发生异常也返回成功，确保用户体验
            log.warn("Token吊销异常: {}", e.getMessage());
            return Result.success("Logout successful");
        }
    }
}
