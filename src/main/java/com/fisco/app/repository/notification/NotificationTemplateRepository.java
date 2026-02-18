package com.fisco.app.repository.notification;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.notification.NotificationTemplate;

/**
 * 通知模板Repository
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {

    /**
     * 根据模板代码查找模板
     */
    Optional<NotificationTemplate> findByCode(String code);

    /**
     * 根据类型查找模板
     */
    List<NotificationTemplate> findByType(String type);

    /**
     * 根据分类查找模板
     */
    List<NotificationTemplate> findByCategory(String category);

    /**
     * 根据类型和分类查找模板
     */
    List<NotificationTemplate> findByTypeAndCategory(String type, String category);

    /**
     * 查找所有启用的模板
     */
    List<NotificationTemplate> findByIsEnabledTrue();

    /**
     * 根据类型查找启用的模板
     */
    List<NotificationTemplate> findByTypeAndIsEnabledTrue(String type);

    /**
     * 检查模板代码是否存在
     */
    boolean existsByCode(String code);
}
