package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.site.dto.PageSectionRequestDTO;
import com.company.officialwebsite.modules.site.dto.PageSectionSortItemDTO;
import com.company.officialwebsite.modules.site.dto.PageSectionStatusDTO;
import com.company.officialwebsite.modules.site.dto.PageSectionVisibilityDTO;
import com.company.officialwebsite.modules.site.vo.PageSectionVO;
import java.util.List;

public interface PageSectionService {

    PageResult<PageSectionVO> getAdminSections(String pageCode, Integer pageNo, Integer pageSize);

    PageSectionVO getAdminSection(Long id);

    Long createSection(PageSectionRequestDTO requestDTO);

    PageSectionVO updateSection(Long id, PageSectionRequestDTO requestDTO);

    void deleteSection(Long id, Integer version);

    PageSectionVO updateVisibility(Long id, PageSectionVisibilityDTO requestDTO);

    PageSectionVO updateStatus(Long id, PageSectionStatusDTO requestDTO);

    void batchSort(List<PageSectionSortItemDTO> sortItems);

    List<PageSectionVO> getPortalSections(String pageCode);
}
