package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.modules.pagebuilder.model.PreviewTokenData;

import java.time.LocalDateTime;

/**
 * PreviewTokenService：受控预览 Token 的生命周期管理服务。
 * <p>
 * Token 为随机 UUID，绑定 pageId / schemaHash / 操作人，存入 Redis，TTL 1 小时。
 * 所有日志输出 Token 时只打印前 6 位掩码，禁止记录完整 Token。
 * </p>
 */
public interface PreviewTokenService {

    /**
     * 为指定草稿生成受控预览 Token，写入 Redis 并绑定元数据。
     *
     * @param pageId    页面定义 ID
     * @param schemaHash 草稿当前 SHA-256 哈希（从 PageDraftEntity 获取）
     * @param createdBy  操作员用户名
     * @return 随机 UUID 格式的预览 Token（不含任何业务数据）
     */
    String createToken(Long pageId, String schemaHash, String createdBy);

    /**
     * 解析 Token，返回绑定的元数据。Token 不存在、已过期或已撤销时抛业务异常。
     *
     * @param token 前端传入的预览 Token
     * @return 绑定的元数据对象
     * @throws com.company.officialwebsite.common.exception.BusinessException Token 无效时抛 PAGE_PREVIEW_TOKEN_EXPIRED
     */
    PreviewTokenData resolveToken(String token);

    /**
     * 主动撤销（删除）Token，使预览链接立即失效。Token 不存在时静默成功（幂等）。
     *
     * @param token 要撤销的预览 Token
     */
    void revokeToken(String token);

    /**
     * 根据 TTL 常量计算 Token 的到期时间，供响应体返回给前端。
     *
     * @return 当前时间加 TTL 后的到期时间
     */
    LocalDateTime computeExpiresAt();
}
