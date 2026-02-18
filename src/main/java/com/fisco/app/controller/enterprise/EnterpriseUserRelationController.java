package com.fisco.app.controller.enterprise;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.dto.enterprise.EnterpriseWithUsersDTO;
import com.fisco.app.dto.enterprise.UserWithEnterpriseDTO;
import com.fisco.app.service.enterprise.EnterpriseService;
import com.fisco.app.service.user.UserService;
import com.fisco.app.vo.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 企业和用户关联查询Controller
 * 提供企业和用户的关联查询功能
 */
@Slf4j
@RestController
@RequestMapping("/api/relations")
@RequiredArgsConstructor
@Api(tags = "企业和用户关联查询")
public class EnterpriseUserRelationController {

    private final UserService userService;
    private final EnterpriseService enterpriseService;

    /**
     * 获取用户及其企业信息
     * GET /api/relations/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @ApiOperation(value = "获取用户及其企业信息", notes = "根据用户ID查询用户详情和所属企业信息")
    public Result<UserWithEnterpriseDTO> getUserWithEnterprise(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId) {
        log.info("获取用户及其企业信息: userId={}", userId);
        UserWithEnterpriseDTO result = userService.getUserWithEnterprise(userId);
        return Result.success(result);
    }

    /**
     * 根据用户名获取用户及其企业信息
     * GET /api/relations/user/username/{username}
     */
    @GetMapping("/user/username/{username}")
    @ApiOperation(value = "根据用户名获取用户及其企业信息", notes = "根据用户名查询用户详情和所属企业信息")
    public Result<UserWithEnterpriseDTO> getUserWithEnterpriseByUsername(
            @ApiParam(value = "用户名", required = true) @PathVariable String username) {
        log.info("根据用户名获取用户及其企业信息: username={}", username);
        UserWithEnterpriseDTO result = userService.getUserWithEnterpriseByUsername(username);
        return Result.success(result);
    }

    /**
     * 获取企业及其所有用户
     * GET /api/relations/enterprise/{enterpriseId}
     */
    @GetMapping("/enterprise/{enterpriseId}")
    @ApiOperation(value = "获取企业及其所有用户", notes = "根据企业ID查询企业详情和所有用户列表")
    public Result<EnterpriseWithUsersDTO> getEnterpriseWithUsers(
            @ApiParam(value = "企业ID", required = true) @PathVariable @NonNull String enterpriseId) {
        log.info("获取企业及其所有用户: enterpriseId={}", enterpriseId);
        EnterpriseWithUsersDTO result = enterpriseService.getEnterpriseWithUsers(enterpriseId);
        return Result.success(result);
    }

    /**
     * 根据企业地址获取企业及其所有用户
     * GET /api/relations/enterprise/address/{address}
     */
    @GetMapping("/enterprise/address/{address}")
    @ApiOperation(value = "根据地址获取企业及其所有用户", notes = "根据区块链地址查询企业详情和所有用户列表")
    public Result<EnterpriseWithUsersDTO> getEnterpriseWithUsersByAddress(
            @ApiParam(value = "企业地址", required = true) @PathVariable String address) {
        log.info("根据地址获取企业及其所有用户: address={}", address);
        EnterpriseWithUsersDTO result = enterpriseService.getEnterpriseWithUsersByAddress(address);
        return Result.success(result);
    }

    /**
     * 获取当前登录用户/企业及其关联信息
     * GET /api/relations/me
     *
     * 根据登录类型返回不同内容：
     * - 用户登录（USER）：返回用户及其企业信息
     * - 企业登录（ENTERPRISE）：返回企业及其用户列表
     * - 管理员登录（ADMIN）：返回管理员信息
     */
    @GetMapping("/me")
    @ApiOperation(value = "获取当前登录信息",
                  notes = "根据登录类型返回：用户登录返回用户+企业信息，企业登录返回企业+用户列表")
    public Result<?> getCurrentUserWithEnterprise(Authentication authentication) {
        String username = authentication.getName();
        com.fisco.app.security.UserAuthentication userAuth =
            (com.fisco.app.security.UserAuthentication) authentication;
        String loginType = userAuth.getLoginType();

        log.info("获取当前登录信息: username={}, loginType={}", username, loginType);

        try {
            if ("ENTERPRISE".equals(loginType) || "ADMIN".equals(loginType)) {
                // 企业登录：返回企业及其用户列表
                log.info("企业登录查询: username={}", username);
                com.fisco.app.dto.enterprise.EnterpriseWithUsersDTO result =
                    enterpriseService.getEnterpriseWithUsersByUsername(username);
                return Result.success(result);
            } else {
                // 用户登录：返回用户及其企业信息
                log.info("用户登录查询: username={}", username);
                UserWithEnterpriseDTO result = userService.getUserWithEnterpriseByUsername(username);
                return Result.success(result);
            }
        } catch (Exception e) {
            log.error("获取当前登录信息失败: username={}, loginType={}, error={}",
                     username, loginType, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 统计企业的用户数量
     * GET /api/relations/enterprise/{enterpriseId}/user-count
     */
    @GetMapping("/enterprise/{enterpriseId}/user-count")
    @ApiOperation(value = "统计企业用户数量", notes = "统计指定企业的用户总数")
    public Result<Long> countUsersByEnterprise(
            @ApiParam(value = "企业ID", required = true) @PathVariable @NonNull String enterpriseId) {
        log.info("统计企业用户数量: enterpriseId={}", enterpriseId);

        EnterpriseWithUsersDTO enterpriseWithUsers = enterpriseService.getEnterpriseWithUserCount(enterpriseId);
        Long userCount = enterpriseWithUsers.getUserCount() != null
            ? enterpriseWithUsers.getUserCount().longValue()
            : 0L;

        return Result.success(userCount);
    }

    /**
     * 验证用户是否属于指定企业
     * GET /api/relations/user/{userId}/enterprise/{enterpriseId}/verify
     */
    @GetMapping("/user/{userId}/enterprise/{enterpriseId}/verify")
    @ApiOperation(value = "验证用户归属", notes = "验证用户是否属于指定企业")
    public Result<Boolean> verifyUserBelongsToEnterprise(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId,
            @ApiParam(value = "企业ID", required = true) @PathVariable @NonNull String enterpriseId) {
        log.info("验证用户归属: userId={}, enterpriseId={}", userId, enterpriseId);

        boolean belongs = userService.isUserBelongsToEnterprise(userId, enterpriseId);

        return Result.success(belongs);
    }

    /**
     * 获取企业的所有活跃用户
     * GET /api/relations/enterprise/{enterpriseId}/active-users
     */
    @GetMapping("/enterprise/{enterpriseId}/active-users")
    @ApiOperation(value = "获取企业活跃用户", notes = "获取指定企业的所有活跃用户")
    public Result<java.util.List<com.fisco.app.entity.user.User>> getActiveUsersByEnterprise(
            @ApiParam(value = "企业ID", required = true) @PathVariable @NonNull String enterpriseId) {
        log.info("获取企业活跃用户: enterpriseId={}", enterpriseId);
        java.util.List<com.fisco.app.entity.user.User> users = userService.getActiveUsersByEnterpriseId(enterpriseId);
        return Result.success(users);
    }
}
