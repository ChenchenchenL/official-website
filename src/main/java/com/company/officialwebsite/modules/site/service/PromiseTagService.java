package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.site.dto.PromiseTagBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.PromiseTagCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.PromiseTagUpdateRequestDTO;
import com.company.officialwebsite.modules.site.vo.AdminPromiseTagVO;
import java.util.List;

/**
 * PromiseTagService：封装承诺标签的后台维护能力。
 */
public interface PromiseTagService {

    PageResult<AdminPromiseTagVO> getAdminPromiseTagList(int pageNo, int pageSize);

    void createPromiseTag(PromiseTagCreateRequestDTO requestDTO);

    void updatePromiseTag(Long id, PromiseTagUpdateRequestDTO requestDTO);

    void deletePromiseTag(Long id, Integer version);

    void reorderPromiseTags(PromiseTagBatchSortRequestDTO requestDTO);
}
