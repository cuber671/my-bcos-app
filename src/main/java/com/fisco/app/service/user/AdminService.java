package com.fisco.app.service.user;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.entity.user.Admin;
import com.fisco.app.exception.BusinessException;
import com.fisco.app.repository.user.AdminRepository;
import com.fisco.app.security.PasswordUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 管理员Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;

    /**
     * 创建管理员
     */
    @Transactional
    public Admin createAdmin(Admin admin, @NonNull String createdBy) {
        // 检查用户名是否已存在
        if (adminRepository.existsByUsername(admin.getUsername())) {
            throw new BusinessException.AdminNotFoundException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (admin.getEmail() != null && adminRepository.existsByEmail(admin.getEmail())) {
            throw new BusinessException("邮箱已被使用");
        }

        // 检查手机号是否已存在
        if (admin.getPhone() != null && adminRepository.existsByPhone(admin.getPhone())) {
            throw new BusinessException("手机号已被使用");
        }

        // 加密密码
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            admin.setPassword(PasswordUtil.encode(admin.getPassword()));
        }

        admin.setCreatedBy(createdBy);
        Admin saved = adminRepository.save(admin);
        log.info("创建管理员成功: username={}, role={}, createdBy={}",
                admin.getUsername(), admin.getRole(), createdBy);
        return saved;
    }

    /**
     * 根据用户名获取管理员
     */
    @NonNull
    public Admin getAdminByUsername(@NonNull String username) {
        java.util.Optional<Admin> result = adminRepository.findByUsername(username);
        if (!result.isPresent()) {
            throw new BusinessException.AdminNotFoundException("管理员不存在: " + username);
        }
        return java.util.Objects.requireNonNull(result.get());
    }

    /**
     * 根据ID获取管理员
     */
    @NonNull
    public Admin getAdminById(@NonNull String id) {
        java.util.Optional<Admin> result = adminRepository.findById(id);
        if (!result.isPresent()) {
            throw new BusinessException.AdminNotFoundException("管理员不存在: " + id);
        }
        return java.util.Objects.requireNonNull(result.get());
    }

    /**
     * 验证管理员登录
     */
    @NonNull
    public Admin validateLogin(@NonNull String username, @NonNull String password) {
        Admin admin = getAdminByUsername(username);

        // 检查账户是否可用
        if (!admin.isAvailable()) {
            if (admin.getStatus() == Admin.AdminStatus.LOCKED) {
                throw new BusinessException("账户已被锁定，请30分钟后再试或联系管理员");
            }
            throw new BusinessException("账户已被禁用，请联系管理员");
        }

        // 验证密码
        if (!PasswordUtil.matches(password, admin.getPassword())) {
            admin.incrementFailedAttempts();
            adminRepository.save(admin);
            log.warn("管理员登录失败: username={}, failedAttempts={}",
                    username, admin.getFailedLoginAttempts());
            throw new BusinessException("用户名或密码错误");
        }

        return admin;
    }

    /**
     * 更新管理员最后登录信息
     */
    @Transactional
    public void updateLastLogin(@NonNull String adminId, @NonNull String ip) {
        Admin admin = getAdminById(adminId);
        admin.updateLoginInfo(ip);
        adminRepository.save(admin);
        log.info("更新管理员登录信息: adminId={}, ip={}", adminId, ip);
    }

    /**
     * 更新管理员信息
     */
    @Transactional
    @NonNull
    public Admin updateAdmin(@NonNull String adminId, Admin updatedAdmin, @NonNull String updatedBy) {
        Admin admin = getAdminById(adminId);

        // 更新允许修改的字段
        if (updatedAdmin.getEmail() != null && !updatedAdmin.getEmail().equals(admin.getEmail())) {
            if (adminRepository.existsByEmail(updatedAdmin.getEmail())) {
                throw new BusinessException("邮箱已被使用");
            }
            admin.setEmail(updatedAdmin.getEmail());
        }

        if (updatedAdmin.getPhone() != null && !updatedAdmin.getPhone().equals(admin.getPhone())) {
            if (adminRepository.existsByPhone(updatedAdmin.getPhone())) {
                throw new BusinessException("手机号已被使用");
            }
            admin.setPhone(updatedAdmin.getPhone());
        }

        if (updatedAdmin.getRealName() != null) {
            admin.setRealName(updatedAdmin.getRealName());
        }

        if (updatedAdmin.getRole() != null) {
            admin.setRole(updatedAdmin.getRole());
        }

        if (updatedAdmin.getRemarks() != null) {
            admin.setRemarks(updatedAdmin.getRemarks());
        }

        admin.setUpdatedBy(updatedBy);
        Admin saved = adminRepository.save(admin);
        log.info("更新管理员信息成功: adminId={}, updatedBy={}", adminId, updatedBy);
        return saved;
    }

    /**
     * 修改管理员密码
     */
    @Transactional
    public void changePassword(@NonNull String adminId, @NonNull String oldPassword,
                               @NonNull String newPassword, @NonNull String updatedBy) {
        Admin admin = getAdminById(adminId);

        // 验证旧密码
        if (!PasswordUtil.matches(oldPassword, admin.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        // 设置新密码
        admin.setPassword(PasswordUtil.encode(newPassword));
        admin.setUpdatedBy(updatedBy);
        adminRepository.save(admin);
        log.info("修改管理员密码成功: adminId={}, updatedBy={}", adminId, updatedBy);
    }

    /**
     * 重置管理员密码
     */
    @Transactional
    public void resetPassword(@NonNull String adminId, @NonNull String newPassword, @NonNull String updatedBy) {
        Admin admin = getAdminById(adminId);
        admin.setPassword(PasswordUtil.encode(newPassword));
        admin.setUpdatedBy(updatedBy);
        adminRepository.save(admin);
        log.info("重置管理员密码成功: adminId={}, updatedBy={}", adminId, updatedBy);
    }

    /**
     * 更新管理员状态
     */
    @Transactional
    public void updateAdminStatus(@NonNull String adminId, Admin.AdminStatus status, @NonNull String updatedBy) {
        Admin admin = getAdminById(adminId);
        admin.setStatus(status);
        admin.setUpdatedBy(updatedBy);
        adminRepository.save(admin);
        log.info("更新管理员状态成功: adminId={}, status={}, updatedBy={}", adminId, status, updatedBy);
    }

    /**
     * 删除管理员
     */
    @Transactional
    public void deleteAdmin(@NonNull String adminId, @NonNull String deletedBy) {
        Admin admin = getAdminById(adminId);
        adminRepository.delete(admin);
        log.info("删除管理员成功: adminId={}, deletedBy={}", adminId, deletedBy);
    }

    /**
     * 获取所有管理员（分页）
     */
    public Page<Admin> getAllAdmins(@NonNull Pageable pageable) {
        return adminRepository.findAll(pageable);
    }

    /**
     * 根据角色查询管理员
     */
    public List<Admin> getAdminsByRole(Admin.AdminRole role) {
        return adminRepository.findAll((Specification<Admin>) (root, query, cb) ->
                cb.equal(root.get("role"), role));
    }

    /**
     * 搜索管理员
     */
    @NonNull
    public Page<Admin> searchAdmins(String keyword, @NonNull Pageable pageable) {
        return adminRepository.findAll((Specification<Admin>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String likeKeyword = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("username"), likeKeyword),
                        cb.like(root.get("realName"), likeKeyword),
                        cb.like(root.get("email"), likeKeyword),
                        cb.like(root.get("phone"), likeKeyword)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    /**
     * 检查用户名是否存在
     */
    public boolean existsByUsername(String username) {
        return adminRepository.existsByUsername(username);
    }

    /**
     * 检查邮箱是否存在
     */
    public boolean existsByEmail(String email) {
        return adminRepository.existsByEmail(email);
    }
}
