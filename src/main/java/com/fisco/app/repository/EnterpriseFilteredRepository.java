package com.fisco.app.repository;

import com.fisco.app.security.UserAuthentication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * 企业过滤Repository基类
 * 提供自动企业数据过滤功能
 *
 * 使用示例：
 * <pre>
 * &#64;Repository
 * public interface UserRepository extends JpaRepository<User, String>, EnterpriseFilteredRepository<User> {
 *     // 自定义查询方法会自动添加企业过滤
 * }
 * </pre>
 */
public interface EnterpriseFilteredRepository<T> {

    /**
     * 查询当前企业可访问的数据
     * 自动根据当前用户的企业ID过滤数据
     *
     * @param entityClass 实体类
     * @param enterpriseIdField 企业ID字段名（默认为"enterpriseId"）
     * @return 过滤后的数据列表
     */
    default List<T> findAllByCurrentEnterprise(Class<T> entityClass, String enterpriseIdField) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication instanceof UserAuthentication)) {
            throw new RuntimeException("未登录或认证信息无效");
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;

        // 系统管理员可以查看所有数据
        if (userAuth.isSystemAdmin()) {
            return findAll(); // 返回所有数据，不过滤
        }

        // 获取当前用户的企业ID
        String currentUserEnterpriseId = userAuth.getEnterpriseId();
        if (currentUserEnterpriseId == null) {
            throw new RuntimeException("当前用户不属于任何企业");
        }

        // 根据企业ID过滤数据
        return findByEnterpriseId(currentUserEnterpriseId);
    }

    /**
     * 根据企业ID查询数据
     * 子类需要实现此方法
     */
    List<T> findByEnterpriseId(String enterpriseId);

    /**
     * 查询所有数据
     * 子类需要实现此方法
     */
    List<T> findAll();
}
