package com.fisco.app.repository.user;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.user.User;


/**
 * 用户Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据手机号查找用户
     */
    Optional<User> findByPhone(String phone);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据企业ID查找所有用户
     */
    List<User> findByEnterpriseId(String enterpriseId);

    /**
     * 根据企业ID和状态查找用户
     */
    List<User> findByEnterpriseIdAndStatus(String enterpriseId, User.UserStatus status);

    /**
     * 根据用户类型查找用户
     */
    List<User> findByUserType(User.UserType userType);

    /**
     * 根据状态查找用户
     */
    List<User> findByStatus(User.UserStatus status);

    /**
     * 查找所有活跃用户
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' ORDER BY u.createdAt DESC")
    List<User> findAllActiveUsers();

    /**
     * 根据企业ID查找活跃用户
     */
    @Query("SELECT u FROM User u WHERE u.enterpriseId = :enterpriseId AND u.status = 'ACTIVE'")
    List<User> findActiveUsersByEnterpriseId(@Param("enterpriseId") String enterpriseId);

    /**
     * 统计企业用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.enterpriseId = :enterpriseId")
    Long countByEnterpriseId(@Param("enterpriseId") String enterpriseId);

    /**
     * 根据真实姓名模糊查询
     */
    @Query("SELECT u FROM User u WHERE u.realName LIKE %:keyword%")
    List<User> findByRealNameContaining(@Param("keyword") String keyword);

    /**
     * 根据用户ID查询用户及其企业信息（使用JOIN FETCH避免N+1查询）
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.enterprise WHERE u.id = :userId")
    Optional<User> findByIdWithEnterprise(@Param("userId") String userId);

    /**
     * 根据用户名查询用户及其企业信息（使用JOIN FETCH避免N+1查询）
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.enterprise WHERE u.username = :username")
    Optional<User> findByUsernameWithEnterprise(@Param("username") String username);

    /**
     * 根据企业ID查询所有用户及其企业信息（使用JOIN FETCH避免N+1查询）
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.enterprise e WHERE u.enterpriseId = :enterpriseId")
    List<User> findByEnterpriseIdWithEnterprise(@Param("enterpriseId") String enterpriseId);

    /**
     * 检查用户是否存在且属于指定企业
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.id = :userId AND u.enterpriseId = :enterpriseId")
    boolean existsByIdAndEnterpriseId(@Param("userId") String userId, @Param("enterpriseId") String enterpriseId);

    /**
     * 根据企业ID删除所有用户
     */
    void deleteByEnterpriseId(String enterpriseId);
}
