package com.company.officialwebsite.modules.product.service;

import com.company.officialwebsite.common.dto.DetailDraftSaveDTO;
import com.company.officialwebsite.common.dto.DetailOfflineDTO;
import com.company.officialwebsite.common.dto.DetailPublishDTO;
import com.company.officialwebsite.common.dto.DetailRollbackDTO;
import com.company.officialwebsite.modules.product.vo.IndustrySolutionDraftVO;
import com.company.officialwebsite.modules.product.vo.IndustrySolutionVersionVO;
import com.company.officialwebsite.modules.product.vo.PortalIndustrySolutionDetailVO;

import java.util.List;

/**
 * IndustrySolutionEditorService：行业解决方案详情编辑全生命周期服务接口。
 */
public interface IndustrySolutionEditorService {

    IndustrySolutionDraftVO createDraftShell(String operator);

    IndustrySolutionDraftVO getDraft(Long solutionId);

    IndustrySolutionDraftVO saveDraft(Long solutionId, DetailDraftSaveDTO saveDTO, String lockToken, String operator);

    String createPreviewToken(Long solutionId, String draftHash, String lockToken, String operator);

    Object renderPreview(Long solutionId, String previewToken, String operator);

    void revokePreviewToken(Long solutionId, String previewToken, String lockToken, String operator);

    IndustrySolutionVersionVO publish(Long solutionId, DetailPublishDTO publishDTO, String lockToken, String operator);

    List<IndustrySolutionVersionVO> listVersions(Long solutionId);

    IndustrySolutionVersionVO rollback(Long solutionId, Long targetVersionId, DetailRollbackDTO rollbackDTO, String lockToken, String operator);

    void offline(Long solutionId, DetailOfflineDTO offlineDTO, String lockToken, String operator);

    PortalIndustrySolutionDetailVO getPortalSolutionDetail(Long solutionId);
}
