package com.fisco.app.event;
import lombok.Getter;

/**
 * 用户更新事件
 * 当用户信息（角色、企业）变更时发布此事件
 */
@Getter
public class UserUpdatedEvent {

    private final String username;
    private final String enterpriseId;
    private final String oldEnterpriseId;
    private final String userId;

    public UserUpdatedEvent(String username, String enterpriseId,
                           String oldEnterpriseId, String userId) {
        this.username = username;
        this.enterpriseId = enterpriseId;
        this.oldEnterpriseId = oldEnterpriseId;
        this.userId = userId;
    }
}
