package com.company.officialwebsite.modules.content.service;

import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.modules.content.dto.DetailPreviewTokenData;

import java.time.LocalDateTime;

/**
 * DetailPreviewTokenService：受控详情（产品、案例、行业方案）预览 Token 的生命周期管理接口。
 */
public interface DetailPreviewTokenService {

    /**
     * 为指定的详情实体生成受控预览 Token。
     *
     * @param resourceType 资源类型（PRODUCT, CASE, INDUSTRY_SOLUTION）
     * @param resourceId   实体 ID
     * @param draftHash    草稿配置 SHA-256 哈希
     * @param createdBy    创建预览的管理员账号
     * @return 64 字符/UUID 随机 Preview Token
     */
    String createToken(EditorResourceTypeEnum resourceType, Long resourceId, String draftHash, String createdBy);

    /**
     * 从 Redis 中解析预览 Token 的关联数据。
     *
     * @param token 预览 Token
     * @return 绑定的 Token 关联数据
     * @throws com.company.officialwebsite.common.exception.BusinessException Token 不存在或已过期时抛出 DETAIL_PREVIEW_TOKEN_EXPIRED
     */
    DetailPreviewTokenData resolveToken(String token);

    /**
     * 主动撤销预览 Token。
     *
     * @param token 预览 Token
     */
    void revokeToken(String token);

    /**
     * 计算该 Preview Token 的过期时间戳。
     *
     * @return 到期时间点
     */
    LocalDateTime computeExpiresAt();
}
