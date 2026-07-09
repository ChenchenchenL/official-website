package com.company.officialwebsite.modules.content.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.content.dto.ContentTagBatchSortRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentTagCreateRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentTagUpdateRequestDTO;
import com.company.officialwebsite.modules.content.vo.AdminContentTagVO;

public interface ContentTagService {

    PageResult<AdminContentTagVO> getAdminContentTagList(int pageNo, int pageSize);

    void createContentTag(ContentTagCreateRequestDTO requestDTO);

    void updateContentTag(Long id, ContentTagUpdateRequestDTO requestDTO);

    void deleteContentTag(Long id, Integer version);

    void reorderContentTags(ContentTagBatchSortRequestDTO requestDTO);
}
