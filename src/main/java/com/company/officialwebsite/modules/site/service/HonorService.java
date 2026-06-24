package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.modules.site.dto.HonorBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.HonorCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.HonorDeleteRequestDTO;
import com.company.officialwebsite.modules.site.dto.HonorUpdateRequestDTO;
import com.company.officialwebsite.modules.site.vo.AdminHonorVO;
import com.company.officialwebsite.modules.site.vo.PortalHonorVO;
import java.util.List;

/**
 * HonorService：封装企业荣誉标签的后台维护和前台读取能力。
 */
public interface HonorService {

    List<AdminHonorVO> getAdminHonors();

    List<AdminHonorVO> createHonor(HonorCreateRequestDTO requestDTO);

    List<AdminHonorVO> updateHonor(Long honorId, HonorUpdateRequestDTO requestDTO);

    List<AdminHonorVO> deleteHonor(Long honorId, HonorDeleteRequestDTO requestDTO);

    List<AdminHonorVO> reorderHonors(HonorBatchSortRequestDTO requestDTO);

    List<PortalHonorVO> getPortalHonors();
}
