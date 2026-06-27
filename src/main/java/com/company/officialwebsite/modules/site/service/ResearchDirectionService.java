package com.company.officialwebsite.modules.site.service;

import java.util.List;
import com.company.officialwebsite.modules.site.dto.ResearchDirectionBatchSortDTO;
import com.company.officialwebsite.modules.site.dto.ResearchDirectionCreateDTO;
import com.company.officialwebsite.modules.site.dto.ResearchDirectionUpdateDTO;
import com.company.officialwebsite.modules.site.vo.AdminResearchDirectionVO;
import com.company.officialwebsite.modules.site.vo.PortalResearchDirectionVO;

/**
 * ResearchDirectionService：重点研发方向管理业务接口。
 */
public interface ResearchDirectionService {

    List<AdminResearchDirectionVO> getAdminDirections();

    List<AdminResearchDirectionVO> createDirection(ResearchDirectionCreateDTO requestDTO);

    List<AdminResearchDirectionVO> updateDirection(Long id, ResearchDirectionUpdateDTO requestDTO);

    List<AdminResearchDirectionVO> deleteDirection(Long id, Integer version);

    List<AdminResearchDirectionVO> batchSortDirections(ResearchDirectionBatchSortDTO requestDTO);

    List<PortalResearchDirectionVO> getPortalDirections();
}
