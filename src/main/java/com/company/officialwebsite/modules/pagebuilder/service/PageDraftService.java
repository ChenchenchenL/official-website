package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.modules.pagebuilder.dto.PageDraftSaveDTO;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PagePreviewVO;

/**
 * PageDraftService：页面草稿管理与 Redis 预览 Token 核心业务接口。
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
    PageDraftVO saveDraft(Long pageId, PageDraftSaveDTO dto);

    /**
     * 为指定页面生成一次性预览 Token，并将当前草稿 Schema 写入 Redis（TTL 10 分钟）。
     *
     * @param pageId 页面定义 ID
     * @return UUID 格式的预览 Token 字符串
     * @throws com.company.officialwebsite.common.exception.BusinessException 草稿不存在或 Schema 为空时抛出 PAGE_DRAFT_NOT_FOUND
     */
    String generatePreviewToken(Long pageId);

    /**
     * 通过预览 Token 从 Redis 中读取对应的草稿 Schema 并构造预览数据 VO。
     *
     * @param previewToken 预览 Token（UUID 格式）
     * @return 包含 pageKey、name 及完整 Schema 的预览 VO
     * @throws com.company.officialwebsite.common.exception.BusinessException Token 不存在或已过期时抛出 PAGE_PREVIEW_TOKEN_EXPIRED
     */
    PagePreviewVO getPreviewData(String previewToken);
}
