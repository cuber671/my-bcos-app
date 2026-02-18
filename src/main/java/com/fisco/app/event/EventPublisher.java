package com.fisco.app.event;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 事件发布服务
 * 用于发布权限相关的更新事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发布用户更新事件
     */
    public void publishUserUpdatedEvent(String username, String enterpriseId,
                                        String oldEnterpriseId, String userId) {
        UserUpdatedEvent event = new UserUpdatedEvent(username, enterpriseId, oldEnterpriseId, userId);
        eventPublisher.publishEvent(event);
        log.debug("已发布用户更新事件: username={}, enterpriseId={}", username, enterpriseId);
    }

    /**
     * 发布企业更新事件
     */
    public void publishEnterpriseUpdatedEvent(String enterpriseId, String enterpriseName) {
        EnterpriseUpdatedEvent event = new EnterpriseUpdatedEvent(enterpriseId, enterpriseName);
        eventPublisher.publishEvent(event);
        log.debug("已发布企业更新事件: enterpriseId={}", enterpriseId);
    }

    /**
     * 发布用户删除事件
     */
    public void publishUserDeletedEvent(String username, String userId, String enterpriseId) {
        UserDeletedEvent event = new UserDeletedEvent(username, userId, enterpriseId);
        eventPublisher.publishEvent(event);
        log.debug("已发布用户删除事件: username={}, userId={}", username, userId);
    }
}
