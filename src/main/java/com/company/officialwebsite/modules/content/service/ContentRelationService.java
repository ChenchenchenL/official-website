package com.company.officialwebsite.modules.content.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.content.dto.ContentRelationCreateRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentRelationUpdateRequestDTO;
import com.company.officialwebsite.modules.content.vo.AdminContentRelationVO;

public interface ContentRelationService {

    PageResult<AdminContentRelationVO> getAdminContentRelationList(int pageNo, int pageSize);

    void createContentRelation(ContentRelationCreateRequestDTO requestDTO);

    void updateContentRelation(Long id, ContentRelationUpdateRequestDTO requestDTO);

    void deleteContentRelation(Long id, Integer version);
}
