package com.company.officialwebsite.modules.casecenter.service;

import com.company.officialwebsite.common.dto.DetailDraftSaveDTO;
import com.company.officialwebsite.common.dto.DetailOfflineDTO;
import com.company.officialwebsite.common.dto.DetailPublishDTO;
import com.company.officialwebsite.common.dto.DetailRollbackDTO;
import com.company.officialwebsite.modules.casecenter.vo.CaseDraftVO;
import com.company.officialwebsite.modules.casecenter.vo.CaseVersionVO;

import java.util.List;

/**
 * CaseEditorService：标杆案例详情编辑全生命周期服务接口。
 */
public interface CaseEditorService {

    CaseDraftVO createDraftShell(String operator);

    CaseDraftVO getDraft(Long caseId);

    CaseDraftVO saveDraft(Long caseId, DetailDraftSaveDTO saveDTO, String lockToken, String operator);

    String createPreviewToken(Long caseId, String draftHash, String lockToken, String operator);

    Object renderPreview(Long caseId, String previewToken, String operator);

    void revokePreviewToken(Long caseId, String previewToken, String lockToken, String operator);

    CaseVersionVO publish(Long caseId, DetailPublishDTO publishDTO, String lockToken, String operator);

    List<CaseVersionVO> listVersions(Long caseId);

    CaseVersionVO rollback(Long caseId, Long targetVersionId, DetailRollbackDTO rollbackDTO, String lockToken, String operator);

    void offline(Long caseId, DetailOfflineDTO offlineDTO, String lockToken, String operator);
}
