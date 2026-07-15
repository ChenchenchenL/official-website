package com.company.officialwebsite.modules.product.service;

import com.company.officialwebsite.common.dto.DetailDraftSaveDTO;
import com.company.officialwebsite.common.dto.DetailOfflineDTO;
import com.company.officialwebsite.common.dto.DetailPublishDTO;
import com.company.officialwebsite.common.dto.DetailRollbackDTO;
import com.company.officialwebsite.modules.product.vo.ProductDraftVO;
import com.company.officialwebsite.modules.product.vo.ProductVersionVO;

import java.util.List;

/**
 * ProductEditorService：产品详情编辑全生命周期服务接口。
 */
public interface ProductEditorService {

    ProductDraftVO createDraftShell(String operator);

    ProductDraftVO getDraft(Long productId);

    ProductDraftVO saveDraft(Long productId, DetailDraftSaveDTO saveDTO, String lockToken, String operator);

    String createPreviewToken(Long productId, String draftHash, String lockToken, String operator);

    Object renderPreview(Long productId, String previewToken, String operator);

    void revokePreviewToken(Long productId, String previewToken, String lockToken, String operator);

    ProductVersionVO publish(Long productId, DetailPublishDTO publishDTO, String lockToken, String operator);

    List<ProductVersionVO> listVersions(Long productId);

    ProductVersionVO rollback(Long productId, Long targetVersionId, DetailRollbackDTO rollbackDTO, String lockToken, String operator);

    void offline(Long productId, DetailOfflineDTO offlineDTO, String lockToken, String operator);
}
