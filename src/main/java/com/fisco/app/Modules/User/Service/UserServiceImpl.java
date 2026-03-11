package com.fisco.app.Modules.User.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fisco.app.Modules.Enterprise.Service.EnterpriseService;
import com.fisco.app.Modules.User.Entity.User;
import com.fisco.app.Modules.User.Enums.UserRoleEnum;
import com.fisco.app.Modules.User.Enums.UserStatusEnum;
import com.fisco.app.Modules.User.Mapper.UserMapper;

/**
 * 用户业务服务实现类
 *
 * 实现用户注册、登录、状态管理等业务功能
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EnterpriseService enterpriseService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ==================== 用户注册与登录 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerUser(String username, String password, String inviteCode,
            String realName, String phone, String email) {

        // 参数校验
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        validatePassword(password);
        if (inviteCode == null || inviteCode.isEmpty()) {
            throw new IllegalArgumentException("邀请码不能为空");
        }
        if (realName == null || realName.isEmpty()) {
            throw new IllegalArgumentException("真实姓名不能为空");
        }

        // 校验邀请码有效性，获取企业ID
        if (!enterpriseService.validateInvitationCode(inviteCode)) {
            throw new IllegalArgumentException("邀请码无效或已过期");
        }
        Long enterpriseId = enterpriseService.useInvitationCode(inviteCode);
        if (enterpriseId == null) {
            throw new IllegalArgumentException("邀请码已被使用");
        }

        // 检查用户名是否已存在
        User existUser = getUserByUsername(username);
        if (existUser != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 检查手机号是否已存在
        if (phone != null && !phone.isEmpty()) {
            User existPhone = getUserByPhone(phone);
            if (existPhone != null) {
                throw new IllegalArgumentException("手机号已被注册");
            }
        }

        // 创建用户对象
        User user = new User();
        user.setEnterpriseId(enterpriseId);
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setUsername(username);
        // 密码加密存储
        user.setPassword(passwordEncoder.encode(password));
        user.setUserRole(UserRoleEnum.OPERATOR.getValue()); // 默认业务员
        user.setStatus(UserStatusEnum.PENDING.getValue()); // 待审核状态

        // 保存到数据库
        userMapper.insert(user);

        logger.info("用户注册成功: userId={}, username={}, enterpriseId={}",
                user.getUserId(), username, enterpriseId);

        return user.getUserId();
    }

    @Override
    public User login(String username, String password) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        User user = getUserByUsername(username);
        if (user == null) {
            return null;
        }

        // 检查用户状态
        if (user.getStatus() == UserStatusEnum.FROZEN.getValue()) {
            throw new IllegalStateException("账户已被冻结");
        }
        if (user.getStatus() == UserStatusEnum.CANCELLED.getValue()) {
            throw new IllegalStateException("账户已注销");
        }
        if (user.getStatus() == UserStatusEnum.PENDING.getValue()) {
            throw new IllegalStateException("账户待审核，暂不能登录");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }

        // 更新最后登录时间
        updateLastLoginTime(user.getUserId());

        return user;
    }

    // ==================== 用户查询 ====================

    @Override
    public User getUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        return userMapper.selectById(userId);
    }

    @Override
    public User getUserByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public User getUserByPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public List<User> getUsersByEnterpriseId(Long enterpriseId) {
        if (enterpriseId == null) {
            return null;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEnterpriseId, enterpriseId);
        wrapper.orderByDesc(User::getCreateTime);
        return userMapper.selectList(wrapper);
    }

    // ==================== 密码管理 ====================

    @Override
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        logger.info("用户密码已更新: userId={}", userId);
        return true;
    }

    // ==================== 用户状态管理 ====================

    @Override
    public boolean updateUserStatus(Long userId, Integer newStatus) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        user.setStatus(newStatus);
        int result = userMapper.updateById(user);

        logger.info("用户状态已更新: userId={}, status={}", userId, newStatus);
        return result > 0;
    }

    @Override
    public boolean freezeUser(Long userId) {
        return updateUserStatus(userId, UserStatusEnum.FROZEN.getValue());
    }

    @Override
    public boolean unfreezeUser(Long userId) {
        return updateUserStatus(userId, UserStatusEnum.NORMAL.getValue());
    }

    @Override
    public boolean disableUser(Long userId) {
        return freezeUser(userId);
    }

    // ==================== 用户审核管理 ====================

    @Override
    public List<User> getPendingUsers(Long enterpriseId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, UserStatusEnum.PENDING.getValue());
        if (enterpriseId != null) {
            wrapper.eq(User::getEnterpriseId, enterpriseId);
        }
        return userMapper.selectList(wrapper);
    }

    @Override
    public boolean auditUser(Long userId, boolean approved) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getStatus() != UserStatusEnum.PENDING.getValue()) {
            throw new IllegalArgumentException("该用户不是待审核状态，无法重复审核");
        }

        // 审核通过设为正常(2)，审核拒绝设为冻结(3)
        int newStatus = approved ? UserStatusEnum.NORMAL.getValue() : UserStatusEnum.FROZEN.getValue();
        user.setStatus(newStatus);
        int result = userMapper.updateById(user);

        logger.info("用户审核完成: userId={}, approved={}, newStatus={}", userId, approved, newStatus);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 物理删除
        int result = userMapper.deleteById(userId);

        logger.info("用户已删除: userId={}", userId);
        return result > 0;
    }

    // ==================== 用户信息管理 ====================

    @Override
    public boolean updateUserInfo(User user) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }

        // 不允许修改的字段
        User existingUser = getUserById(user.getUserId());
        if (existingUser == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 只允许修改非敏感字段
        existingUser.setRealName(user.getRealName());
        existingUser.setPhone(user.getPhone());
        existingUser.setEmail(user.getEmail());

        int result = userMapper.updateById(existingUser);

        logger.info("用户信息已更新: userId={}", user.getUserId());
        return result > 0;
    }

    @Override
    public boolean updateUserRole(Long userId, String userRole) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 校验角色是否有效
        try {
            UserRoleEnum.valueOf(userRole);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的用户角色: " + userRole);
        }

        user.setUserRole(userRole);
        int result = userMapper.updateById(user);

        logger.info("用户角色已更新: userId={}, role={}", userId, userRole);
        return result > 0;
    }

    @Override
    public void updateLastLoginTime(Long userId) {
        User user = getUserById(userId);
        if (user != null) {
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }

    // ==================== 用户注销管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CancellationResult applyCancellation(Long userId, String reason, String password) {
        CancellationResult result = new CancellationResult();

        // 查询用户信息
        User user = getUserById(userId);
        if (user == null) {
            result.setSuccess(false);
            result.setMessage("用户不存在");
            return result;
        }

        // 检查用户状态是否为正常
        if (user.getStatus() != UserStatusEnum.NORMAL.getValue()) {
            result.setSuccess(false);
            result.setMessage("用户状态异常，无法申请注销");
            return result;
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            result.setSuccess(false);
            result.setMessage("密码验证失败");
            return result;
        }

        // 检查是否是财务角色，有未处理任务则驳回
        if (UserRoleEnum.FINANCE.getValue().equals(user.getUserRole())) {
            // TODO: 检查是否有未处理的审批任务
            // 暂时跳过，后续集成工作流模块
        }

        // 更新用户状态为注销待审核（等待管理员审核）
        user.setStatus(UserStatusEnum.PENDING_CANCEL.getValue());
        userMapper.updateById(user);

        result.setSuccess(true);
        result.setMessage("注销申请已提交，等待管理员审核");
        result.setUserId(userId);
        result.setReason(reason);
        result.setApplyTime(LocalDateTime.now());

        logger.info("用户注销申请成功: userId={}, reason={}", userId, reason);
        return result;
    }

    @Override
    public boolean revokeCancellation(Long userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 只有注销中或注销待审核状态才能撤回
        if (user.getStatus() != UserStatusEnum.CANCELLING.getValue()
            && user.getStatus() != UserStatusEnum.PENDING_CANCEL.getValue()) {
            throw new IllegalArgumentException("只有注销中的用户才能撤回申请");
        }

        // 更新状态为正常
        user.setStatus(UserStatusEnum.NORMAL.getValue());
        userMapper.updateById(user);

        logger.info("用户注销申请已撤回: userId={}", userId);
        return true;
    }

    @Override
    public List<User> getPendingCancellationUsers(Long enterpriseId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, UserStatusEnum.PENDING_CANCEL.getValue());
        if (enterpriseId != null) {
            wrapper.eq(User::getEnterpriseId, enterpriseId);
        }
        return userMapper.selectList(wrapper);
    }

    @Override
    public boolean auditCancellation(Long userId, boolean approved) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getStatus() != UserStatusEnum.PENDING_CANCEL.getValue()) {
            throw new IllegalArgumentException("该用户不是注销待审核状态，无法审核");
        }

        // 审核通过设为已注销(5)，审核拒绝恢复正常(2)
        int newStatus = approved ? UserStatusEnum.CANCELLED.getValue() : UserStatusEnum.NORMAL.getValue();
        user.setStatus(newStatus);
        int result = userMapper.updateById(user);

        logger.info("用户注销审核完成: userId={}, approved={}, newStatus={}", userId, approved, newStatus);
        return result > 0;
    }

    /**
     * 校验密码强度
     *
     * @param password 密码
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("密码长度不能少于8位");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("密码必须包含大写字母");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("密码必须包含小写字母");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("密码必须包含数字");
        }
    }
}
