package com.fisco.app.Modules.User.Service;

import java.util.List;

import com.fisco.app.Modules.User.Entity.User;

/**
 * 用户业务服务接口
 *
 * 提供用户注册、登录、信息管理等业务功能
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface UserService {

    // ==================== 用户注册与登录 ====================

    /**
     * 用户注册（员工入职）
     *
     * @param username 用户名
     * @param password 登录密码
     * @param inviteCode 邀请码
     * @param realName 真实姓名
     * @param phone 手机号
     * @param email 邮箱
     * @return 注册成功的用户ID
     */
    Long registerUser(String username, String password, String inviteCode,
            String realName, String phone, String email);

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回用户信息，失败返回null
     */
    User login(String username, String password);

    // ==================== 用户查询 ====================

    /**
     * 根据ID查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Long userId);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    User getUserByPhone(String phone);

    /**
     * 根据企业ID查询用户列表
     *
     * @param enterpriseId 企业ID
     * @return 用户列表
     */
    List<User> getUsersByEnterpriseId(Long enterpriseId);

    // ==================== 密码管理 ====================

    /**
     * 修改登录密码
     *
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean updatePassword(Long userId, String oldPassword, String newPassword);

    // ==================== 用户状态管理 ====================

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param newStatus 新状态
     * @return 是否成功
     */
    boolean updateUserStatus(Long userId, Integer newStatus);

    /**
     * 冻结用户
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean freezeUser(Long userId);

    /**
     * 解冻用户
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean unfreezeUser(Long userId);

    /**
     * 禁用用户（强制下线）
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean disableUser(Long userId);

    /**
     * 删除用户（离职）
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteUser(Long userId);

    // ==================== 用户信息管理 ====================

    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return 是否成功
     */
    boolean updateUserInfo(User user);

    /**
     * 更新用户角色
     *
     * @param userId 用户ID
     * @param userRole 新角色
     * @return 是否成功
     */
    boolean updateUserRole(Long userId, String userRole);

    /**
     * 更新最后登录时间
     *
     * @param userId 用户ID
     */
    void updateLastLoginTime(Long userId);

    // ==================== 用户注销管理 ====================

    /**
     * 获取待审核用户列表
     *
     * @param enterpriseId 企业ID（可选，不传则查询所有）
     * @return 待审核用户列表
     */
    List<User> getPendingUsers(Long enterpriseId);

    /**
     * 审核用户注册申请
     *
     * @param userId 用户ID
     * @param approved 审核结果：true-通过(设为正常), false-拒绝(设为冻结)
     * @return 审核结果
     */
    boolean auditUser(Long userId, boolean approved);

    /**
     * 发起注销申请
     *
     * @param userId 用户ID
     * @param reason 注销原因
     * @param password 登录密码
     * @return 注销申请结果
     */
    CancellationResult applyCancellation(Long userId, String reason, String password);

    /**
     * 撤回注销申请
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean revokeCancellation(Long userId);

    /**
     * 获取待审核注销用户列表
     *
     * @param enterpriseId 企业ID（可选，不传则查询所有）
     * @return 待审核注销用户列表
     */
    List<User> getPendingCancellationUsers(Long enterpriseId);

    /**
     * 审核用户注销申请
     *
     * @param userId 用户ID
     * @param approved 审核结果：true-通过(设为已注销), false-拒绝(恢复正常)
     * @return 审核结果
     */
    boolean auditCancellation(Long userId, boolean approved);

    // ==================== 内部类 ====================

    /**
     * 注销申请结果
     */
    class CancellationResult {
        private boolean success;
        private String message;
        private Long userId;
        private String reason;
        private java.time.LocalDateTime applyTime;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public java.time.LocalDateTime getApplyTime() { return applyTime; }
        public void setApplyTime(java.time.LocalDateTime applyTime) { this.applyTime = applyTime; }
    }
}
