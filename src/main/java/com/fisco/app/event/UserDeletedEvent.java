package com.fisco.app.event;
import lombok.Getter;

/**
 * 用户删除事件
 * 当用户被删除时发布此事件
 */
@Getter
public class UserDeletedEvent {

    private final String username;
    private final String userId;
    private final String enterpriseId;

    public UserDeletedEvent(String username, String userId, String enterpriseId) {
        this.username = username;
        this.userId = userId;
        this.enterpriseId = enterpriseId;
    }
}
