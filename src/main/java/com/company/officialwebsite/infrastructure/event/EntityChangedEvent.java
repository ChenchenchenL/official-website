package com.company.officialwebsite.infrastructure.event;

import org.springframework.context.ApplicationEvent;

/**
 * EntityChangedEvent：业务实体变更领域事件。
 * 当 product、casecenter、site、media 等模块的实体发生更新或删除时，
 * 由对应的 Service 在事务内发布此事件，Application 层监听器在事务提交后
 * 查询 cms_page_dependency 并清理相关页面缓存。
 */
public class EntityChangedEvent extends ApplicationEvent {

    /** 变更所属业务模块，如 "product", "casecenter", "site", "media" */
    private final String targetModule;

    /** 变更目标实体类型，如 "Product", "Case", "SiteConfig" */
    private final String targetEntityType;

    /** 变更目标实体唯一标识，统一转为字符串 */
    private final String targetEntityId;

    private EntityChangedEvent(Object source, String targetModule, String targetEntityType, String targetEntityId) {
        super(source);
        this.targetModule = targetModule;
        this.targetEntityType = targetEntityType;
        this.targetEntityId = targetEntityId;
    }

    /**
     * 静态工厂方法，便于各业务 Service 创建事件实例。
     *
     * @param source           事件发布者（通常为 this）
     * @param targetModule     业务模块标识
     * @param targetEntityType 实体类型标识
     * @param targetEntityId   实体主键（String 形式）
     */
    public static EntityChangedEvent of(Object source, String targetModule,
                                        String targetEntityType, String targetEntityId) {
        return new EntityChangedEvent(source, targetModule, targetEntityType, targetEntityId);
    }

    public String getTargetModule() {
        return targetModule;
    }

    public String getTargetEntityType() {
        return targetEntityType;
    }

    public String getTargetEntityId() {
        return targetEntityId;
    }
}
