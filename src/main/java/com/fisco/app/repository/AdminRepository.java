package com.fisco.app.repository;

import com.fisco.app.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 管理员Repository
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, String>, JpaSpecificationExecutor<Admin> {

    /**
     * 根据用户名查找管理员
     */
    Optional<Admin> findByUsername(String username);

    /**
     * 根据邮箱查找管理员
     */
    Optional<Admin> findByEmail(String email);

    /**
     * 根据手机号查找管理员
     */
    Optional<Admin> findByPhone(String phone);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查手机号是否存在
     */
    boolean existsByPhone(String phone);
}
