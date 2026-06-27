package com.company.officialwebsite.modules.casecenter.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.casecenter.dto.CaseBatchSortDTO;
import com.company.officialwebsite.modules.casecenter.dto.CaseCreateDTO;
import com.company.officialwebsite.modules.casecenter.dto.CaseDeleteDTO;
import com.company.officialwebsite.modules.casecenter.dto.CaseUpdateDTO;
import com.company.officialwebsite.modules.casecenter.vo.AdminCaseVO;
import com.company.officialwebsite.modules.casecenter.vo.PortalCaseVO;
import java.util.List;

/**
 * CaseService：标杆案例管理业务接口。
 */
public interface CaseService {

    PageResult<AdminCaseVO> getAdminCaseList(int pageNo, int pageSize);

    List<AdminCaseVO> createCase(CaseCreateDTO createDTO);

    List<AdminCaseVO> updateCase(Long id, CaseUpdateDTO updateDTO);

    List<AdminCaseVO> deleteCase(Long id, CaseDeleteDTO deleteDTO);

    List<AdminCaseVO> batchSortCases(CaseBatchSortDTO sortDTO);

    List<PortalCaseVO> getPortalCases();
}
