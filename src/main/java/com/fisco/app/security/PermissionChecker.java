package com.fisco.app.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.fisco.app.exception.BusinessException;
import com.fisco.app.service.system.PermissionAuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * 权限验证工具类
 * 提供企业级别和角色级别的权限检查，并记录审计日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private final PermissionAuditService auditService;

    /**
     * 检查当前用户是否可以操作指定企业的用户
     *
     * @param authentication 认证信息
     * @param targetEnterpriseId 目标企业ID
     * @param request HTTP请求（可选，用于审计日志）
     * @throws BusinessException 如果没有权限则抛出异常
     */
    public void checkEnterprisePermission(Authentication authentication, String targetEnterpriseId,
                                          HttpServletRequest request) {
        if (!(authentication instanceof UserAuthentication)) {
            log.warn("Authentication is not UserAuthentication: {}", authentication.getClass());
            auditService.logAccessDenied(authentication, "ENTERPRISE_ACCESS", targetEnterpriseId,
                    "checkEnterprisePermission", "无效的认证信息", request);
            throw new BusinessException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;

        // 系统管理员可以操作所有企业
        if (userAuth.isSystemAdmin()) {
            log.debug("System admin accessing enterprise: {}", targetEnterpriseId);
            auditService.logAccessGranted(authentication, "ENTERPRISE_ACCESS", targetEnterpriseId,
                    "checkEnterprisePermission", request);
            return;
        }

        // 检查企业ID是否匹配
        if (!userAuth.belongsToEnterprise(targetEnterpriseId)) {
            String denialReason = String.format("用户属于企业 %s，试图访问企业 %s",
                    userAuth.getEnterpriseId(), targetEnterpriseId);
            log.warn("User {} attempted to access enterprise {}, but belongs to {}",
                    userAuth.getName(), targetEnterpriseId, userAuth.getEnterpriseId());
            auditService.logAccessDenied(authentication, "ENTERPRISE_ACCESS", targetEnterpriseId,
                    "checkEnterprisePermission", denialReason, request);
            throw new BusinessException("无权限访问该企业的数据");
        }

        log.debug("User {} has permission to access enterprise {}",
                 userAuth.getName(), targetEnterpriseId);
        auditService.logAccessGranted(authentication, "ENTERPRISE_ACCESS", targetEnterpriseId,
                "checkEnterprisePermission", request);
    }

    /**
     * 检查当前用户是否可以操作指定企业的用户（无HTTP请求）
     */
    public void checkEnterprisePermission(Authentication authentication, String targetEnterpriseId) {
        checkEnterprisePermission(authentication, targetEnterpriseId, null);
    }

    /**
     * 检查当前用户是否有企业管理员权限
     *
     * @param authentication 认证信息
     * @throws BusinessException 如果不是企业管理员则抛出异常
     */
    public void checkEnterpriseAdminPermission(Authentication authentication) {
        if (!(authentication instanceof UserAuthentication)) {
            throw new BusinessException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;

        if (!userAuth.isEnterpriseAdmin() && !userAuth.isSystemAdmin()) {
            log.warn("User {} attempted admin action without permission", userAuth.getName());
            throw new BusinessException("需要企业管理员权限");
        }

        log.debug("User {} has enterprise admin permission", userAuth.getName());
    }

    /**
     * 检查当前用户是否可以审核指定企业的用户注册
     *
     * @param authentication 认证信息
     * @param userEnterpriseId 待审核用户所属企业ID
     * @param request HTTP请求（可选，用于审计日志）
     * @throws BusinessException 如果没有权限则抛出异常
     */
    public void checkUserApprovalPermission(Authentication authentication, String userEnterpriseId,
                                            HttpServletRequest request) {
        if (!(authentication instanceof UserAuthentication)) {
            auditService.logAccessDenied(authentication, "USER_APPROVAL", userEnterpriseId,
                    "checkUserApprovalPermission", "无效的认证信息", request);
            throw new BusinessException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;

        // 系统管理员可以审核所有企业的用户
        if (userAuth.isSystemAdmin()) {
            log.debug("System admin approving user from enterprise: {}", userEnterpriseId);
            auditService.logAccessGranted(authentication, "USER_APPROVAL", userEnterpriseId,
                    "checkUserApprovalPermission", request);
            return;
        }

        // 企业管理员只能审核本企业的用户
        if (!userAuth.isEnterpriseAdmin()) {
            String denialReason = String.format("用户 %s 不是企业管理员", userAuth.getName());
            log.warn("User {} is not an enterprise admin", userAuth.getName());
            auditService.logAccessDenied(authentication, "USER_APPROVAL", userEnterpriseId,
                    "checkUserApprovalPermission", denialReason, request);
            throw new BusinessException("需要企业管理员权限才能审核用户");
        }

        if (!userAuth.belongsToEnterprise(userEnterpriseId)) {
            String denialReason = String.format("用户属于企业 %s，试图审核企业 %s 的用户",
                    userAuth.getEnterpriseId(), userEnterpriseId);
            log.warn("User {} from enterprise {} attempted to approve user from enterprise {}",
                    userAuth.getName(), userAuth.getEnterpriseId(), userEnterpriseId);
            auditService.logAccessDenied(authentication, "USER_APPROVAL", userEnterpriseId,
                    "checkUserApprovalPermission", denialReason, request);
            throw new BusinessException("只能审核本企业的用户注册申请");
        }

        log.debug("User {} has permission to approve users from enterprise {}",
                 userAuth.getName(), userEnterpriseId);
        auditService.logAccessGranted(authentication, "USER_APPROVAL", userEnterpriseId,
                "checkUserApprovalPermission", request);
    }

    /**
     * 检查当前用户是否可以审核指定企业的用户注册（无HTTP请求）
     */
    public void checkUserApprovalPermission(Authentication authentication, String userEnterpriseId) {
        checkUserApprovalPermission(authentication, userEnterpriseId, null);
    }

    /**
     * 检查当前用户是否有系统管理员权限
     *
     * @param authentication 认证信息
     * @throws BusinessException 如果不是系统管理员则抛出异常
     */
    public void checkSystemAdminPermission(Authentication authentication) {
        if (!(authentication instanceof UserAuthentication)) {
            throw new BusinessException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;

        if (!userAuth.isSystemAdmin()) {
            log.warn("User {} attempted system admin action without permission", userAuth.getName());
            throw new BusinessException("需要系统管理员权限");
        }

        log.debug("User {} has system admin permission", userAuth.getName());
    }

    /**
     * 获取当前用户的企业ID
     *
     * @param authentication 认证信息
     * @return 企业ID，如果未设置则返回null
     * @throws BusinessException 如果认证信息无效
     */
    public String getCurrentUserEnterpriseId(Authentication authentication) {
        if (!(authentication instanceof UserAuthentication)) {
            throw new BusinessException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;
        return userAuth.getEnterpriseId();
    }

    /**
     * 获取当前用户的用户名
     *
     * @param authentication 认证信息
     * @return 用户名
     * @throws BusinessException 如果认证信息无效
     */
    public String getCurrentUsername(Authentication authentication) {
        if (!(authentication instanceof UserAuthentication)) {
            throw new BusinessException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;
        return userAuth.getName();
    }

    /**
     * 检查当前用户是否可以创建仓单
     * 任何已认证企业都可以创建仓单（货主、供应商等），但仓储企业不能创建仓单
     *
     * @param authentication 认证信息
     * @param ownerEnterpriseId 货主企业ID
     * @param request HTTP请求（可选，用于审计日志）
     * @throws BusinessException 如果没有权限则抛出异常
     */
    public void checkCreateReceiptPermission(Authentication authentication, String ownerEnterpriseId,
                                            HttpServletRequest request) {
        if (!(authentication instanceof UserAuthentication)) {
            auditService.logAccessDenied(authentication, "CREATE_RECEIPT", ownerEnterpriseId,
                    "checkCreateReceiptPermission", "无效的认证信息", request);
            throw new BusinessException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;

        // 系统管理员可以为任何企业创建仓单
        if (userAuth.isSystemAdmin()) {
            log.debug("System admin creating receipt for enterprise: {}", ownerEnterpriseId);
            auditService.logAccessGranted(authentication, "CREATE_RECEIPT", ownerEnterpriseId,
                    "checkCreateReceiptPermission", request);
            return;
        }

        // 业务规则：仓储企业不能创建仓单（只能审核仓单）
        if (userAuth.isWarehouseProvider()) {
            String denialReason = "仓储企业不能创建仓单，只能审核仓单";
            log.warn("Warehouse provider {} attempted to create receipt, which is not allowed",
                    userAuth.getName());
            auditService.logAccessDenied(authentication, "CREATE_RECEIPT", ownerEnterpriseId,
                    "checkCreateReceiptPermission", denialReason, request);
            throw new BusinessException("仓储企业不能创建仓单，只能审核仓单");
        }

        // 检查企业ID是否匹配（只能为本企业创建仓单）
        if (!userAuth.belongsToEnterprise(ownerEnterpriseId)) {
            String denialReason = String.format("用户属于企业 %s，试图为企业 %s 创建仓单",
                    userAuth.getEnterpriseId(), ownerEnterpriseId);
            log.warn("User {} attempted to create receipt for enterprise {}, but belongs to {}",
                    userAuth.getName(), ownerEnterpriseId, userAuth.getEnterpriseId());
            auditService.logAccessDenied(authentication, "CREATE_RECEIPT", ownerEnterpriseId,
                    "checkCreateReceiptPermission", denialReason, request);
            throw new BusinessException("只能为本企业创建仓单");
        }

        log.debug("User {} has permission to create receipt for enterprise {}",
                 userAuth.getName(), ownerEnterpriseId);
        auditService.logAccessGranted(authentication, "CREATE_RECEIPT", ownerEnterpriseId,
                "checkCreateReceiptPermission", request);
    }

    /**
     * 检查当前用户是否可以审核指定仓储企业的仓单
     * 只有指定的仓储企业（WAREHOUSE_PROVIDER角色）才能审核仓单
     *
     * @param authentication 认证信息
     * @param warehouseEnterpriseId 仓储企业ID
     * @param request HTTP请求（可选，用于审计日志）
     * @throws BusinessException 如果没有权限则抛出异常
     */
    public void checkReceiptApprovalPermission(Authentication authentication, String warehouseEnterpriseId,
                                             HttpServletRequest request) {
        if (!(authentication instanceof UserAuthentication)) {
            auditService.logAccessDenied(authentication, "APPROVE_RECEIPT", warehouseEnterpriseId,
                    "checkReceiptApprovalPermission", "无效的认证信息", request);
            throw new BusinessException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;

        // 系统管理员可以审核所有仓单
        if (userAuth.isSystemAdmin()) {
            log.debug("System admin approving receipt for warehouse: {}", warehouseEnterpriseId);
            auditService.logAccessGranted(authentication, "APPROVE_RECEIPT", warehouseEnterpriseId,
                    "checkReceiptApprovalPermission", request);
            return;
        }

        // 检查企业ID是否匹配（只有指定的仓储企业可以审核）
        if (!userAuth.belongsToEnterprise(warehouseEnterpriseId)) {
            String denialReason = String.format("用户属于企业 %s，试图审核企业 %s 的仓单",
                    userAuth.getEnterpriseId(), warehouseEnterpriseId);
            log.warn("User {} attempted to approve receipt for warehouse {}, but belongs to {}",
                    userAuth.getName(), warehouseEnterpriseId, userAuth.getEnterpriseId());
            auditService.logAccessDenied(authentication, "APPROVE_RECEIPT", warehouseEnterpriseId,
                    "checkReceiptApprovalPermission", denialReason, request);
            throw new BusinessException("只有指定的仓储企业可以审核此仓单");
        }

        log.debug("User {} has permission to approve receipt for warehouse {}",
                 userAuth.getName(), warehouseEnterpriseId);
        auditService.logAccessGranted(authentication, "APPROVE_RECEIPT", warehouseEnterpriseId,
                "checkReceiptApprovalPermission", request);
    }

    /**
     * 检查当前用户是否可以操作仓单（查询、更新、删除等）
     *
     * @param authentication 认证信息
     * @param ownerEnterpriseId 货主企业ID
     * @param warehouseEnterpriseId 仓储企业ID
     * @param request HTTP请求（可选，用于审计日志）
     * @throws BusinessException 如果没有权限则抛出异常
     */
    public void checkReceiptAccessPermission(Authentication authentication, String ownerEnterpriseId,
                                           String warehouseEnterpriseId, HttpServletRequest request) {
        if (!(authentication instanceof UserAuthentication)) {
            auditService.logAccessDenied(authentication, "ACCESS_RECEIPT",
                    ownerEnterpriseId + "/" + warehouseEnterpriseId,
                    "checkReceiptAccessPermission", "无效的认证信息", request);
            throw new BusinessException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;

        // 系统管理员可以操作所有仓单
        if (userAuth.isSystemAdmin()) {
            log.debug("System admin accessing receipt");
            auditService.logAccessGranted(authentication, "ACCESS_RECEIPT",
                    ownerEnterpriseId + "/" + warehouseEnterpriseId,
                    "checkReceiptAccessPermission", request);
            return;
        }

        // 检查是否为货主企业或仓储企业
        boolean isOwner = userAuth.belongsToEnterprise(ownerEnterpriseId);
        boolean isWarehouse = userAuth.belongsToEnterprise(warehouseEnterpriseId);

        if (!isOwner && !isWarehouse) {
            String denialReason = String.format("用户属于企业 %s，试图操作企业 %s（货主）或 %s（仓储）的仓单",
                    userAuth.getEnterpriseId(), ownerEnterpriseId, warehouseEnterpriseId);
            log.warn("User {} attempted to access receipt without permission", userAuth.getName());
            auditService.logAccessDenied(authentication, "ACCESS_RECEIPT",
                    ownerEnterpriseId + "/" + warehouseEnterpriseId,
                    "checkReceiptAccessPermission", denialReason, request);
            throw new BusinessException("无权限访问此仓单");
        }

        log.debug("User {} has permission to access receipt (owner={}, warehouse={})",
                 userAuth.getName(), isOwner, isWarehouse);
        auditService.logAccessGranted(authentication, "ACCESS_RECEIPT",
                ownerEnterpriseId + "/" + warehouseEnterpriseId,
                "checkReceiptAccessPermission", request);
    }

    /**
     * 检查当前用户是否是仓单的持单人
     * 用于背书转让、冻结、拆分等需要持单人权限的操作
     *
     * @param authentication 认证信息
     * @param holderAddress 仓单的持单人地址
     * @param operation 操作名称（如"背书转让"、"冻结仓单"、"拆分仓单"）
     * @param request HTTP请求（可选，用于审计日志）
     * @throws BusinessException 如果当前用户不是持单人则抛出异常
     */
    public void checkHolderPermission(Authentication authentication, String holderAddress,
                                     String operation, HttpServletRequest request) {
        if (!(authentication instanceof UserAuthentication)) {
            auditService.logAccessDenied(authentication, operation, holderAddress,
                    "checkHolderPermission", "无效的认证信息", request);
            throw new BusinessException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;

        // 系统管理员可以执行所有操作
        if (userAuth.isSystemAdmin()) {
            log.debug("System admin performing {} on receipt held by {}", operation, holderAddress);
            auditService.logAccessGranted(authentication, operation, holderAddress,
                    "checkHolderPermission", request);
            return;
        }

        // 检查是否为持单人（通过区块链地址匹配）
        if (!userAuth.isHolder(holderAddress)) {
            String denialReason = String.format("用户地址 %s 不是仓单持单人 %s",
                    userAuth.getEnterpriseAddress(), holderAddress);
            log.warn("User {} attempted to {} receipt held by {}, user address is {}",
                    userAuth.getName(), operation, holderAddress, userAuth.getEnterpriseAddress());
            auditService.logAccessDenied(authentication, operation, holderAddress,
                    "checkHolderPermission", denialReason, request);
            throw new BusinessException("只有当前持单人可以" + operation);
        }

        log.debug("User {} has permission to {} receipt (holder={})",
                 userAuth.getName(), operation, holderAddress);
        auditService.logAccessGranted(authentication, operation, holderAddress,
                "checkHolderPermission", request);
    }

    /**
     * 检查当前用户是否是仓单的持单人（无HTTP请求）
     */
    public void checkHolderPermission(Authentication authentication, String holderAddress,
                                     String operation) {
        checkHolderPermission(authentication, holderAddress, operation, null);
    }

    /**
     * 获取当前用户的区块链地址
     *
     * @param authentication 认证信息
     * @return 区块链地址，如果未设置则返回null
     * @throws BusinessException 如果认证信息无效
     */
    public String getCurrentUserAddress(Authentication authentication) {
        if (!(authentication instanceof UserAuthentication)) {
            throw new BusinessException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;
        return userAuth.getEnterpriseAddress();
    }
}
