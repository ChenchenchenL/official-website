package com.company.officialwebsite.modules.content.service;

import com.company.officialwebsite.common.dto.DetailDraftSaveDTO;
import com.company.officialwebsite.common.dto.DetailOfflineDTO;
import com.company.officialwebsite.common.dto.DetailPublishDTO;
import com.company.officialwebsite.common.dto.DetailRollbackDTO;
import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import java.util.Objects;

/**
 * AbstractDetailEditorService：三类独立内容详情（产品、案例、行业方案）编辑框架通用抽象基类。
 *
 * @param <D> 领域草稿对象类型
 * @param <V> 领域版本快照对象类型
 */
public abstract class AbstractDetailEditorService<D, V> {

    private final EditorLockService editorLockService;

    protected AbstractDetailEditorService(EditorLockService editorLockService) {
        this.editorLockService = Objects.requireNonNull(editorLockService, "editorLockService must not be null");
    }

    /**
     * 获取资源类型枚举。
     */
    public abstract EditorResourceTypeEnum getResourceType();

    /**
     * 查询指定实体的当前草稿。
     *
     * @param resourceId 实体 ID
     * @return 实体草稿 DTO/VO 封装
     */
    public abstract D getDraft(Long resourceId);

    /**
     * 保存/更新指定实体的草稿。
     *
     * @param resourceId 实体 ID
     * @param saveDTO    草稿保存参数
     * @param operator   当前操作管理员
     * @return 更新后的草稿
     */
    public final D saveDraft(Long resourceId, DetailDraftSaveDTO saveDTO, String lockToken, String operator) {
        validateEditorLock(resourceId, lockToken, operator);
        return doSaveDraft(resourceId, saveDTO, operator);
    }

    /**
     * 在独占编辑锁已校验后保存草稿。
     */
    protected abstract D doSaveDraft(Long resourceId, DetailDraftSaveDTO saveDTO, String operator);

    /**
     * 发布指定实体的草稿上线。
     *
     * @param resourceId 实体 ID
     * @param publishDTO 发布变更说明与版本
     * @param operator   发布管理员
     * @return 生成的版本快照
     */
    public final V publish(Long resourceId, DetailPublishDTO publishDTO, String lockToken, String operator) {
        validateEditorLock(resourceId, lockToken, operator);
        return doPublish(resourceId, publishDTO, operator);
    }

    /**
     * 在独占编辑锁已校验后发布草稿。
     */
    protected abstract V doPublish(Long resourceId, DetailPublishDTO publishDTO, String operator);

    /**
     * 将指定实体回滚到历史版本。
     *
     * @param resourceId      实体 ID
     * @param targetVersionId 目标历史版本 ID
     * @param rollbackDTO     回滚说明与当前乐观锁版本
     * @param operator        操作管理员
     * @return 生成的新版本快照
     */
    public final V rollback(
            Long resourceId,
            Long targetVersionId,
            DetailRollbackDTO rollbackDTO,
            String lockToken,
            String operator) {
        validateEditorLock(resourceId, lockToken, operator);
        return doRollback(resourceId, targetVersionId, rollbackDTO, operator);
    }

    /**
     * 在独占编辑锁已校验后回滚历史版本。
     */
    protected abstract V doRollback(
            Long resourceId,
            Long targetVersionId,
            DetailRollbackDTO rollbackDTO,
            String operator);

    /**
     * 下线指定实体的已发布版本。
     *
     * @param resourceId 实体 ID
     * @param offlineDTO 下线原因与版本
     * @param operator   操作管理员
     */
    public final void offline(Long resourceId, DetailOfflineDTO offlineDTO, String lockToken, String operator) {
        validateEditorLock(resourceId, lockToken, operator);
        doOffline(resourceId, offlineDTO, operator);
    }

    /**
     * 在独占编辑锁已校验后下线详情。
     */
    protected abstract void doOffline(Long resourceId, DetailOfflineDTO offlineDTO, String operator);

    /**
     * 统一的受保护写操作门禁，子类无法绕过该模板方法执行草稿、发布、回滚或下线。
     */
    private void validateEditorLock(Long resourceId, String lockToken, String operator) {
        editorLockService.validateLock(getResourceType(), resourceId, lockToken, operator);
    }
}
