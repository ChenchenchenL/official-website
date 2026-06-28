package com.company.officialwebsite.modules.lead.service;

import com.company.officialwebsite.modules.lead.dto.CooperationDirectionTagBatchSortRequestDTO;
import com.company.officialwebsite.modules.lead.dto.CooperationDirectionTagCreateRequestDTO;
import com.company.officialwebsite.modules.lead.dto.CooperationDirectionTagUpdateRequestDTO;
import com.company.officialwebsite.modules.lead.vo.AdminCooperationDirectionTagVO;
import com.company.officialwebsite.modules.lead.vo.PortalCooperationDirectionTagVO;
import java.util.List;

/**
 * CooperationDirectionTagService：封装合作方向标签的后台维护与前台只读能力。
 */
public interface CooperationDirectionTagService {

    List<AdminCooperationDirectionTagVO> getAdminCooperationDirectionTagList();

    void createCooperationDirectionTag(CooperationDirectionTagCreateRequestDTO requestDTO);

    void updateCooperationDirectionTag(Long id, CooperationDirectionTagUpdateRequestDTO requestDTO);

    void deleteCooperationDirectionTag(Long id, Integer version);

    void reorderCooperationDirectionTags(CooperationDirectionTagBatchSortRequestDTO requestDTO);

    List<PortalCooperationDirectionTagVO> getPortalCooperationDirectionTagList();
}
