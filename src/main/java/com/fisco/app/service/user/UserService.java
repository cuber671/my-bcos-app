package com.fisco.app.service.user;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.entity.enterprise.Enterprise;
import com.fisco.app.entity.user.User;
import com.fisco.app.exception.BusinessException;
import com.fisco.app.repository.user.UserRepository;
import com.fisco.app.security.PasswordUtil;
import com.fisco.app.security.PermissionChecker;
import com.fisco.app.service.enterprise.EnterpriseService;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户Service
 */
@Slf4j
@Service
@Api(tags = "用户服务")
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EnterpriseService enterpriseService;
    private final PermissionChecker permissionChecker;
    private final InvitationCodeService invitationCodeService;

    /**
     * 创建用户
     */
    @Transactional
    public User createUser(User user, String createdBy) {
        log.info("==================== 用户创建开始 ====================");
        log.info("用户基本信息: username={}, realName={}, email={}, phone={}",
                 user.getUsername(), user.getRealName(), user.getEmail(), user.getPhone());
        log.info("企业信息: enterpriseId={}, userType={}", user.getEnterpriseId(), user.getUserType());

        long startTime = System.currentTimeMillis();

        try {
            // 检查用户名是否已存在
            log.debug("检查用户名唯一性: username={}", user.getUsername());
            if (userRepository.existsByUsername(user.getUsername())) {
                log.error("用户名已存在: username={}", user.getUsername());
                throw new BusinessException("用户名已存在");
            }
            log.debug("✓ 用户名唯一性检查通过");

            // 检查邮箱是否已存在
            if (user.getEmail() != null) {
                log.debug("检查邮箱唯一性: email={}", user.getEmail());
                if (userRepository.existsByEmail(user.getEmail())) {
                    log.error("邮箱已被使用: email={}", user.getEmail());
                    throw new BusinessException("邮箱已被使用");
                }
                log.debug("✓ 邮箱唯一性检查通过");
            }

            // 检查手机号是否已存在
            if (user.getPhone() != null) {
                log.debug("检查手机号唯一性: phone={}", user.getPhone());
                if (userRepository.findByPhone(user.getPhone()).isPresent()) {
                    log.error("手机号已被使用: phone={}", user.getPhone());
                    throw new BusinessException("手机号已被使用");
                }
                log.debug("✓ 手机号唯一性检查通过");
            }

            // 验证企业是否存在
            String enterpriseId = user.getEnterpriseId();
            if (enterpriseId != null) {
                log.debug("验证企业存在性: enterpriseId={}", enterpriseId);
                try {
                    enterpriseService.getEnterpriseById(enterpriseId);
                    log.debug("✓ 企业验证通过");
                } catch (Exception e) {
                    log.error("所属企业不存在: enterpriseId={}", enterpriseId);
                    throw new BusinessException("所属企业不存在");
                }
            }

            // 加密密码
            log.debug("加密用户密码");
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                String encodedPassword = PasswordUtil.encode(user.getPassword());
                user.setPassword(encodedPassword);
                user.setPasswordChangedAt(LocalDateTime.now());
                log.debug("✓ 密码加密完成");
            } else {
                log.error("密码不能为空");
                throw new BusinessException("密码不能为空");
            }

            // 设置默认值
            log.debug("设置默认值");
            if (user.getStatus() == null) {
                user.setStatus(User.UserStatus.ACTIVE);
                log.debug("✓ 默认状态: ACTIVE");
            }

            if (user.getUserType() == null) {
                user.setUserType(User.UserType.ENTERPRISE_USER);
                log.debug("✓ 默认用户类型: ENTERPRISE_USER");
            }

            // 保存到数据库
            log.debug("保存用户到数据库");
            User saved = userRepository.save(user);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 用户创建完成: userId={}, username={}, realName={}, 耗时={}ms",
                     saved.getId(), saved.getUsername(), saved.getRealName(), duration);
            log.info("==================== 用户创建结束 ====================");

            return saved;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 用户创建失败: username={}, 耗时={}ms, error={}",
                     user.getUsername(), duration, e.getMessage(), e);
            log.info("==================== 用户创建失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 用户注册（使用邀请码）
     */
    @Transactional
    public User registerUserWithInvitationCode(
            com.fisco.app.dto.user.UserRegistrationRequest request,
            org.springframework.security.core.Authentication authentication) {
        log.info("用户注册: username={}, invitationCode={}",
                 request.getUsername(), request.getInvitationCode());

        // 确定创建者信息
        String createdBy;
        if (authentication != null && authentication.isAuthenticated()) {
            // 如果有token（管理员代注册），使用当前登录用户名
            createdBy = authentication.getName();
            log.info("管理员代用户注册: operator={}, targetUser={}",
                     createdBy, request.getUsername());
        } else {
            // 如果没有token（公开接口自主注册），使用固定标识
            createdBy = "SELF_REGISTER";
            log.info("用户自主注册（公开接口）: username={}", request.getUsername());
        }

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被使用");
        }

        // 检查手机号是否已存在
        if (request.getPhone() != null && userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new BusinessException("手机号已被使用");
        }

        // 验证邀请码
        com.fisco.app.entity.user.InvitationCode invitationCode = invitationCodeService.validateCode(request.getInvitationCode());

        // 检查邀请码所属企业的状态
        Enterprise enterprise = enterpriseService.getEnterpriseById(
            java.util.Objects.requireNonNull(invitationCode.getEnterpriseId()));
        if (enterprise.getStatus() != Enterprise.EnterpriseStatus.ACTIVE) {
            throw new BusinessException("企业未激活，无法注册");
        }

        // 创建用户对象
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setEnterpriseId(invitationCode.getEnterpriseId());
        user.setDepartment(request.getDepartment());
        user.setPosition(request.getPosition());
        user.setInvitationCode(request.getInvitationCode());
        user.setRegistrationRemarks(request.getRemarks());

        // 加密密码
        String encodedPassword = PasswordUtil.encode(user.getPassword());
        user.setPassword(encodedPassword);
        user.setPasswordChangedAt(LocalDateTime.now());

        // 设置为待审核状态
        user.setStatus(User.UserStatus.PENDING);
        user.setUserType(User.UserType.ENTERPRISE_USER);
        user.setCreatedBy(createdBy);
        user.setLoginCount(0);

        // 保存用户
        User saved = userRepository.save(user);

        // 使用邀请码（增加使用次数）
        invitationCodeService.useCode(request.getInvitationCode());

        log.info("用户注册成功，等待企业审核: id={}, username={}, enterpriseId={}, createdBy={}",
                 saved.getId(), saved.getUsername(), saved.getEnterpriseId(), createdBy);

        // 创建返回对象（不包含密码）
        User result = new User();
        result.setId(saved.getId());
        result.setUsername(saved.getUsername());
        result.setRealName(saved.getRealName());
        result.setEmail(saved.getEmail());
        result.setPhone(saved.getPhone());
        result.setEnterpriseId(saved.getEnterpriseId());
        result.setDepartment(saved.getDepartment());
        result.setPosition(saved.getPosition());
        result.setStatus(saved.getStatus());
        result.setUserType(saved.getUserType());
        result.setCreatedBy(saved.getCreatedBy());
        result.setCreatedAt(saved.getCreatedAt());
        result.setUpdatedAt(saved.getUpdatedAt());

        return result;
    }

    /**
     * 企业审核用户注册
     */
    @Transactional
    public void approveUserRegistration(@NonNull String userId, String approver, Authentication authentication) {
        log.info("企业审核用户: userId={}, approver={}", userId, approver);

        User user = getUserById(java.util.Objects.requireNonNull(userId));

        if (user.getStatus() != User.UserStatus.PENDING) {
            throw new BusinessException("用户状态不是待审核状态");
        }

        // 权限验证：检查审核人是否有权限审核该用户
        if (user.getEnterpriseId() != null) {
            permissionChecker.checkUserApprovalPermission(authentication, user.getEnterpriseId());
        }

        // 审核通过，设置为激活状态
        user.setStatus(User.UserStatus.ACTIVE);
        user.setUpdatedBy(approver);
        userRepository.save(user);

        log.info("用户审核通过: userId={}, username={}, enterpriseId={}",
                 userId, user.getUsername(), user.getEnterpriseId());
    }

    /**
     * 企业拒绝用户注册
     */
    @Transactional
    public void rejectUserRegistration(@NonNull String userId, String approver, String reason, Authentication authentication) {
        log.info("企业拒绝用户: userId={}, approver={}, reason={}", userId, approver, reason);

        User user = getUserById(java.util.Objects.requireNonNull(userId));

        if (user.getStatus() != User.UserStatus.PENDING) {
            throw new BusinessException("用户状态不是待审核状态");
        }

        // 权限验证：检查审核人是否有权限审核该用户
        if (user.getEnterpriseId() != null) {
            permissionChecker.checkUserApprovalPermission(authentication, user.getEnterpriseId());
        }

        // 拒绝后设置为禁用状态
        user.setStatus(User.UserStatus.DISABLED);
        user.setRegistrationRemarks(reason != null ? reason : "企业拒绝了注册申请");
        user.setUpdatedBy(approver);
        userRepository.save(user);

        log.info("用户注册已拒绝: userId={}, username={}, reason={}, enterpriseId={}",
                 userId, user.getUsername(), reason, user.getEnterpriseId());
    }

    /**
     * 获取企业的待审核用户列表
     */
    public List<User> getPendingUsersByEnterprise(String enterpriseId) {
        return userRepository.findByEnterpriseIdAndStatus(enterpriseId, User.UserStatus.PENDING);
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public User updateUser(@NonNull String userId, User updatedUser, String updatedBy) {
        log.info("更新用户信息: userId={}", userId);

        User user = getUserById(userId);

        // 检查用户名是否被其他用户占用
        if (!user.getUsername().equals(updatedUser.getUsername()) &&
            userRepository.existsByUsername(updatedUser.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否被其他用户占用
        if (updatedUser.getEmail() != null &&
            !updatedUser.getEmail().equals(user.getEmail()) &&
            userRepository.existsByEmail(updatedUser.getEmail())) {
            throw new BusinessException("邮箱已被使用");
        }

        // 更新允许修改的字段
        user.setRealName(updatedUser.getRealName());
        user.setEmail(updatedUser.getEmail());
        user.setPhone(updatedUser.getPhone());
        user.setDepartment(updatedUser.getDepartment());
        user.setPosition(updatedUser.getPosition());
        user.setAvatarUrl(updatedUser.getAvatarUrl());

        // 支持更新用户类型（需要企业管理员权限）
        if (updatedUser.getUserType() != null && !updatedUser.getUserType().equals(user.getUserType())) {
            log.info("更新用户类型: userId={}, oldType={}, newType={}",
                     userId, user.getUserType(), updatedUser.getUserType());
            user.setUserType(updatedUser.getUserType());
        }

        user.setUpdatedBy(updatedBy);

        User saved = userRepository.save(user);
        log.info("用户信息更新成功: userId={}", userId);

        saved.setPassword(null);
        return saved;
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(@NonNull String userId, String oldPassword, String newPassword) {
        log.info("修改密码: userId={}", userId);

        User user = getUserById(userId);

        // 验证旧密码
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        // 加密新密码
        String encodedPassword = PasswordUtil.encode(newPassword);
        user.setPassword(encodedPassword);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setUpdatedBy(user.getUsername());

        userRepository.save(user);
        log.info("密码修改成功: userId={}", userId);
    }

    /**
     * 重置密码（管理员功能）
     */
    @Transactional
    public void resetPassword(@NonNull String userId, String newPassword, String operator) {
        log.info("重置密码: userId={}, operator={}", userId, operator);

        User user = getUserById(userId);

        String encodedPassword = PasswordUtil.encode(newPassword);
        user.setPassword(encodedPassword);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setUpdatedBy(operator);

        userRepository.save(user);
        log.info("密码重置成功: userId={}", userId);
    }

    /**
     * 启用/禁用用户
     */
    @Transactional
    public void setUserStatus(@NonNull String userId, User.UserStatus status, String operator) {
        log.info("设置用户状态: userId={}, status={}", userId, status);

        User user = getUserById(userId);
        user.setStatus(status);
        user.setUpdatedBy(operator);

        userRepository.save(user);
        log.info("用户状态设置成功: userId={}, status={}", userId, status);
    }

    /**
     * 锁定用户
     */
    @Transactional
    public void lockUser(@NonNull String userId, String operator) {
        setUserStatus(userId, User.UserStatus.LOCKED, operator);
    }

    /**
     * 解锁用户
     */
    @Transactional
    public void unlockUser(@NonNull String userId, String operator) {
        setUserStatus(userId, User.UserStatus.ACTIVE, operator);
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(@NonNull String userId) {
        log.info("删除用户: userId={}", userId);

        if (!userRepository.existsById(userId)) {
            throw new BusinessException.UserNotFoundException(userId);
        }

        userRepository.deleteById(userId);
        log.info("用户删除成功: userId={}", userId);
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(@NonNull String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException.UserNotFoundException(userId));
    }

    /**
     * 根据用户名获取用户
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在: " + username));
    }

    /**
     * 验证用户登录
     */
    public User validateLogin(String username, String password) {
        log.info("用户登录验证: username={}", username);

        User user = getUserByUsername(username);

        // 检查账户状态
        if (user.isLocked()) {
            throw new BusinessException("账户已被锁定，请联系管理员");
        }

        if (user.isDisabled()) {
            throw new BusinessException("账户已被禁用，请联系管理员");
        }

        // 验证密码
        if (!PasswordUtil.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        log.info("用户登录验证成功: username={}", username);
        return user;
    }

    /**
     * 更新最后登录信息
     */
    @Transactional
    public void updateLastLogin(@NonNull String userId, String ip) {
        User user = getUserById(userId);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        user.setLoginCount(user.getLoginCount() + 1);
        userRepository.save(user);
    }

    /**
     * 获取企业的所有用户
     */
    public List<User> getUsersByEnterpriseId(String enterpriseId) {
        return userRepository.findByEnterpriseId(enterpriseId);
    }

    /**
     * 获取企业的所有活跃用户
     */
    public List<User> getActiveUsersByEnterpriseId(String enterpriseId) {
        return userRepository.findActiveUsersByEnterpriseId(enterpriseId);
    }

    /**
     * 根据用户类型获取用户列表
     */
    public List<User> getUsersByType(User.UserType userType) {
        return userRepository.findByUserType(userType);
    }

    /**
     * 分页查询用户
     */
    public Page<User> getUsers(@NonNull Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * 搜索用户（按真实姓名）
     */
    public List<User> searchUsersByRealName(String keyword) {
        return userRepository.findByRealNameContaining(keyword);
    }

    /**
     * 统计企业用户数量
     */
    public Long countUsersByEnterprise(String enterpriseId) {
        return userRepository.countByEnterpriseId(enterpriseId);
    }

    /**
     * 获取用户及其企业信息（使用JOIN FETCH优化查询）
     */
    public com.fisco.app.dto.enterprise.UserWithEnterpriseDTO getUserWithEnterprise(@NonNull String userId) {
        User user = userRepository.findByIdWithEnterprise(userId)
                .orElseThrow(() -> new BusinessException.UserNotFoundException(userId));

        // 如果LAZY加载未触发enterprise，手动查询
        Enterprise enterprise = user.getEnterprise();
        String enterpriseId = user.getEnterpriseId();
        if (enterprise == null && enterpriseId != null) {
            enterprise = enterpriseService.getEnterpriseById(enterpriseId);
        }

        return com.fisco.app.dto.enterprise.UserWithEnterpriseDTO.fromEntities(user, enterprise);
    }

    /**
     * 根据用户名获取用户及其企业信息
     */
    public com.fisco.app.dto.enterprise.UserWithEnterpriseDTO getUserWithEnterpriseByUsername(String username) {
        User user = userRepository.findByUsernameWithEnterprise(username)
                .orElseThrow(() -> new BusinessException("用户不存在: " + username));

        // 如果LAZY加载未触发enterprise，手动查询
        Enterprise enterprise = user.getEnterprise();
        String enterpriseId = user.getEnterpriseId();
        if (enterprise == null && enterpriseId != null) {
            enterprise = enterpriseService.getEnterpriseById(enterpriseId);
        }

        return com.fisco.app.dto.enterprise.UserWithEnterpriseDTO.fromEntities(user, enterprise);
    }

    /**
     * 验证用户是否属于指定企业
     */
    public boolean isUserBelongsToEnterprise(@NonNull String userId, String enterpriseId) {
        return userRepository.existsByIdAndEnterpriseId(userId, enterpriseId);
    }
}
