package com.fisco.app.service.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.dto.user.SetUserPermissionsRequest;
import com.fisco.app.dto.user.UserPermissionDTO;
import com.fisco.app.entity.user.UserPermission;
import com.fisco.app.exception.BusinessException;
import com.fisco.app.repository.user.UserPermissionRepository;
import com.fisco.app.repository.user.UserRepository;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户权限Service
 */
@Slf4j
@Service
@Api(tags = "用户权限服务")
@RequiredArgsConstructor
public class UserPermissionService {

    private final UserPermissionRepository userPermissionRepository;
    private final UserRepository userRepository;

    /**
     * 设置用户权限（替换模式）
     */
    @Transactional
    public List<UserPermissionDTO> setUserPermissions(@NonNull String userId,
                                                       @NonNull List<SetUserPermissionsRequest.PermissionItem> permissions,
                                                       String operator) {
        log.info("设置用户权限: userId={}, permissionsCount={}, operator={}",
                 userId, permissions.size(), operator);

        // 验证用户是否存在
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException.UserNotFoundException(userId));

        // 删除用户的所有现有权限
        log.debug("删除用户现有权限: userId={}", userId);
        userPermissionRepository.deleteByUserId(userId);

        // 创建新权限
        List<UserPermission> newPermissions = new ArrayList<>();
        for (SetUserPermissionsRequest.PermissionItem item : permissions) {
            UserPermission permission = new UserPermission();
            permission.setUserId(userId);
            permission.setPermissionCode(item.getPermissionCode());
            permission.setPermissionName(item.getPermissionName());
            permission.setResourceType(item.getResourceType());
            permission.setOperation(item.getOperation());
            permission.setScope(item.getScope());
            permission.setIsEnabled(item.getIsEnabled());
            permission.setExpireAt(item.getExpireAt());
            permission.setRemarks(item.getRemarks());
            permission.setCreatedBy(operator);

            newPermissions.add(permission);
        }

        // 批量保存
        List<UserPermission> savedPermissions = userPermissionRepository.saveAll(newPermissions);

        log.info("用户权限设置成功: userId={}, permissionsCount={}", userId, savedPermissions.size());

        return savedPermissions.stream()
                .map(UserPermissionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 添加用户权限（追加模式）
     */
    @Transactional
    public UserPermissionDTO addUserPermission(@NonNull String userId,
                                               @NonNull SetUserPermissionsRequest.PermissionItem permission,
                                               String operator) {
        log.info("添加用户权限: userId={}, permissionCode={}", userId, permission.getPermissionCode());

        // 验证用户是否存在
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException.UserNotFoundException(userId));

        // 检查权限是否已存在
        if (userPermissionRepository.findByUserIdAndPermissionCode(userId, permission.getPermissionCode()).isPresent()) {
            throw new BusinessException("用户已拥有该权限: " + permission.getPermissionCode());
        }

        UserPermission userPermission = new UserPermission();
        userPermission.setUserId(userId);
        userPermission.setPermissionCode(permission.getPermissionCode());
        userPermission.setPermissionName(permission.getPermissionName());
        userPermission.setResourceType(permission.getResourceType());
        userPermission.setOperation(permission.getOperation());
        userPermission.setScope(permission.getScope());
        userPermission.setIsEnabled(permission.getIsEnabled());
        userPermission.setExpireAt(permission.getExpireAt());
        userPermission.setRemarks(permission.getRemarks());
        userPermission.setCreatedBy(operator);

        UserPermission saved = userPermissionRepository.save(userPermission);

        log.info("用户权限添加成功: userId={}, permissionId={}", userId, saved.getId());

        return UserPermissionDTO.fromEntity(saved);
    }

    /**
     * 获取用户的所有权限
     */
    public List<UserPermissionDTO> getUserPermissions(@NonNull String userId) {
        log.debug("获取用户权限: userId={}", userId);

        List<UserPermission> permissions = userPermissionRepository.findByUserId(userId);

        return permissions.stream()
                .map(UserPermissionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的有效权限（启用且未过期）
     */
    public List<UserPermissionDTO> getValidUserPermissions(@NonNull String userId) {
        log.debug("获取用户有效权限: userId={}", userId);

        List<UserPermission> permissions = userPermissionRepository
                .findValidPermissionsByUserId(userId, LocalDateTime.now());

        return permissions.stream()
                .map(UserPermissionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否拥有指定权限
     */
    public boolean checkPermission(@NonNull String userId,
                                   @NonNull UserPermission.ResourceType resourceType,
                                   @NonNull UserPermission.Operation operation) {
        return userPermissionRepository.hasPermission(userId, resourceType, operation, LocalDateTime.now());
    }

    /**
     * 检查用户是否拥有指定权限代码
     */
    public boolean checkPermissionByCode(@NonNull String userId, @NonNull String permissionCode) {
        return userPermissionRepository.hasPermissionByCode(userId, permissionCode, LocalDateTime.now());
    }

    /**
     * 删除用户权限
     */
    @Transactional
    public void deleteUserPermission(@NonNull String userId, @NonNull String permissionId, String operator) {
        log.info("删除用户权限: userId={}, permissionId={}, operator={}", userId, permissionId, operator);

        UserPermission permission = userPermissionRepository.findById(permissionId)
                .orElseThrow(() -> new BusinessException("权限不存在: " + permissionId));

        if (!permission.getUserId().equals(userId)) {
            throw new BusinessException("权限不属于该用户");
        }

        userPermissionRepository.delete(permission);

        log.info("用户权限删除成功: permissionId={}", permissionId);
    }

    /**
     * 删除用户的所有权限
     */
    @Transactional
    public void clearUserPermissions(@NonNull String userId, String operator) {
        log.info("清除用户所有权限: userId={}, operator={}", userId, operator);

        userPermissionRepository.deleteByUserId(userId);

        log.info("用户权限清除成功: userId={}", userId);
    }

    /**
     * 启用/禁用用户权限
     */
    @Transactional
    public void toggleUserPermission(@NonNull String userId,
                                     @NonNull String permissionId,
                                     boolean enabled,
                                     String operator) {
        log.info("{}用户权限: userId={}, permissionId={}, operator={}",
                 enabled ? "启用" : "禁用", userId, permissionId, operator);

        UserPermission permission = userPermissionRepository.findById(permissionId)
                .orElseThrow(() -> new BusinessException("权限不存在: " + permissionId));

        if (!permission.getUserId().equals(userId)) {
            throw new BusinessException("权限不属于该用户");
        }

        permission.setIsEnabled(enabled);
        permission.setUpdatedBy(operator);

        userPermissionRepository.save(permission);

        log.info("用户权限{}成功: permissionId={}", enabled ? "启用" : "禁用", permissionId);
    }

    /**
     * 统计用户权限数量
     */
    public Long countUserPermissions(@NonNull String userId) {
        return userPermissionRepository.countByUserId(userId);
    }

    /**
     * 统计用户有效权限数量
     */
    public Long countValidUserPermissions(@NonNull String userId) {
        return userPermissionRepository.countValidPermissionsByUserId(userId, LocalDateTime.now());
    }
}
