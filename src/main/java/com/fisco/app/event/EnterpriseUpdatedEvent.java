package com.fisco.app.event;
import lombok.Getter;

/**
 * 企业更新事件
 * 当企业信息变更时发布此事件
 */
@Getter
public class EnterpriseUpdatedEvent {

    private final String enterpriseId;
    private final String enterpriseName;

    public EnterpriseUpdatedEvent(String enterpriseId, String enterpriseName) {
        this.enterpriseId = enterpriseId;
        this.enterpriseName = enterpriseName;
    }
}
