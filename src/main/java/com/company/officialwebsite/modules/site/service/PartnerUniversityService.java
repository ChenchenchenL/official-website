package com.company.officialwebsite.modules.site.service;

import java.util.List;
import com.company.officialwebsite.modules.site.dto.PartnerUniversityBatchSortDTO;
import com.company.officialwebsite.modules.site.dto.PartnerUniversityCreateDTO;
import com.company.officialwebsite.modules.site.dto.PartnerUniversityUpdateDTO;
import com.company.officialwebsite.modules.site.vo.AdminPartnerUniversityVO;
import com.company.officialwebsite.modules.site.vo.PortalPartnerUniversityVO;

/**
 * PartnerUniversityService：合作高校管理业务接口。
 */
public interface PartnerUniversityService {

    List<AdminPartnerUniversityVO> getAdminUniversities();

    List<AdminPartnerUniversityVO> createUniversity(PartnerUniversityCreateDTO requestDTO);

    List<AdminPartnerUniversityVO> updateUniversity(Long id, PartnerUniversityUpdateDTO requestDTO);

    List<AdminPartnerUniversityVO> deleteUniversity(Long id, Integer version);

    List<AdminPartnerUniversityVO> batchSortUniversities(PartnerUniversityBatchSortDTO requestDTO);

    List<PortalPartnerUniversityVO> getPortalUniversities();
}
