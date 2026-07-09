package com.company.officialwebsite.modules.content.service;

import com.company.officialwebsite.modules.content.dto.ContentCategoryBatchSortRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentCategoryCreateRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentCategoryUpdateRequestDTO;
import com.company.officialwebsite.modules.content.vo.AdminContentCategoryVO;
import java.util.List;

public interface ContentCategoryService {

    List<AdminContentCategoryVO> getAdminContentCategoryTree();

    void createContentCategory(ContentCategoryCreateRequestDTO requestDTO);

    void updateContentCategory(Long id, ContentCategoryUpdateRequestDTO requestDTO);

    void deleteContentCategory(Long id, Integer version);

    void reorderContentCategories(ContentCategoryBatchSortRequestDTO requestDTO);
}
