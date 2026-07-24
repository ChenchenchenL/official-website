package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.modules.pagebuilder.dto.PageDraftSaveDTO;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftVO;

/**
 * PageDraftService：页面草稿管理核心业务接口。
 */
public interface PageDraftService {

    /**
     * 根据页面定义 ID 查询当前草稿。
     *
     * @param pageId 页面定义 ID
     * @return 草稿 VO
     * @throws com.company.officialwebsite.common.exception.BusinessException 草稿不存在时抛出 PAGE_DRAFT_NOT_FOUND
     */
    PageDraftVO getDraft(Long pageId);

    /**
     * 保存/更新指定页面的草稿 Schema。
     * <p>
     * 执行乐观锁版本校验、Schema 合规性校验，并计算 SHA-256 哈希后持久化草稿。
     * </p>
     *
     * @param pageId 页面定义 ID
     * @param dto    包含完整 Schema 和版本号的请求体
     * @return 更新后的草稿 VO
     * @throws com.company.officialwebsite.common.exception.BusinessException 草稿不存在或版本冲突时抛出对应错误码
     */
    PageDraftVO saveDraft(Long pageId, PageDraftSaveDTO dto, String lockToken, String operator);

    /**
     * 将指定页面的草稿配置重置为当前在线 ACTIVE 发布快照。
     *
     * @param pageId    页面定义 ID
     * @param operator  操作员账号
     * @return 重置后的草稿 VO
     */
    PageDraftVO resetDraftToPublished(Long pageId, String lockToken, String operator);

    /**
     * 根据 SchemaModel 直接保存草稿。
     *
     * @param pageId      页面定义 ID
     * @param schemaModel Schema 数据模型
     * @param remark      编辑会话备注
     * @param version     并发乐观锁版本号
     * @param lockToken   编辑锁凭证
     * @param operator    操作员账号
     * @return 更新后的草稿 VO
     */
    PageDraftVO saveDraft(Long pageId, com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel schemaModel, String remark, Integer version, String lockToken, String operator);
}

