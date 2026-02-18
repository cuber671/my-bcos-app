package com.fisco.app.repository.user;

import com.fisco.app.entity.user.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户权限Repository
 */
@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, String> {

    /**
     * 根据用户ID查询所有权限
     */
    List<UserPermission> findByUserId(String userId);

    /**
     * 根据用户ID查询有效权限（启用且未过期）
     */
    @Query("SELECT up FROM UserPermission up WHERE " +
           "up.userId = :userId AND " +
           "up.isEnabled = true AND " +
           "(up.expireAt IS NULL OR up.expireAt > :now)")
    List<UserPermission> findValidPermissionsByUserId(@Param("userId") String userId,
                                                        @Param("now") LocalDateTime now);

    /**
     * 根据用户ID和权限代码查询
     */
    Optional<UserPermission> findByUserIdAndPermissionCode(String userId, String permissionCode);

    /**
     * 根据用户ID和资源类型查询
     */
    List<UserPermission> findByUserIdAndResourceType(String userId, UserPermission.ResourceType resourceType);

    /**
     * 根据用户ID、资源类型和操作类型查询
     */
    Optional<UserPermission> findByUserIdAndResourceTypeAndOperation(
            String userId,
            UserPermission.ResourceType resourceType,
            UserPermission.Operation operation
    );

    /**
     * 检查用户是否拥有指定权限
     */
    @Query("SELECT CASE WHEN COUNT(up) > 0 THEN true ELSE false END FROM UserPermission up WHERE " +
           "up.userId = :userId AND " +
           "up.resourceType = :resourceType AND " +
           "up.operation = :operation AND " +
           "up.isEnabled = true AND " +
           "(up.expireAt IS NULL OR up.expireAt > :now)")
    boolean hasPermission(@Param("userId") String userId,
                          @Param("resourceType") UserPermission.ResourceType resourceType,
                          @Param("operation") UserPermission.Operation operation,
                          @Param("now") LocalDateTime now);

    /**
     * 检查用户是否拥有指定权限代码
     */
    @Query("SELECT CASE WHEN COUNT(up) > 0 THEN true ELSE false END FROM UserPermission up WHERE " +
           "up.userId = :userId AND " +
           "up.permissionCode = :permissionCode AND " +
           "up.isEnabled = true AND " +
           "(up.expireAt IS NULL OR up.expireAt > :now)")
    boolean hasPermissionByCode(@Param("userId") String userId,
                                @Param("permissionCode") String permissionCode,
                                @Param("now") LocalDateTime now);

    /**
     * 删除用户的所有权限
     */
    void deleteByUserId(String userId);

    /**
     * 根据用户ID和操作类型查询权限
     */
    List<UserPermission> findByUserIdAndOperation(String userId, UserPermission.Operation operation);

    /**
     * 统计用户的权限数量
     */
    @Query("SELECT COUNT(up) FROM UserPermission up WHERE up.userId = :userId")
    Long countByUserId(@Param("userId") String userId);

    /**
     * 统计用户的有效权限数量
     */
    @Query("SELECT COUNT(up) FROM UserPermission up WHERE " +
           "up.userId = :userId AND " +
           "up.isEnabled = true AND " +
           "(up.expireAt IS NULL OR up.expireAt > :now)")
    Long countValidPermissionsByUserId(@Param("userId") String userId,
                                       @Param("now") LocalDateTime now);
}
