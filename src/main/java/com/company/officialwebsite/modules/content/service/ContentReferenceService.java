package com.company.officialwebsite.modules.content.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.content.dto.ContentReferenceCreateRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentReferenceUpdateRequestDTO;
import com.company.officialwebsite.modules.content.vo.AdminContentReferenceVO;

public interface ContentReferenceService {

    PageResult<AdminContentReferenceVO> getAdminContentReferenceList(int pageNo, int pageSize);

    void createContentReference(ContentReferenceCreateRequestDTO requestDTO);

    void updateContentReference(Long id, ContentReferenceUpdateRequestDTO requestDTO);

    void deleteContentReference(Long id, Integer version);

    boolean hasActiveReferences(String referencedType, Long referencedId);

    java.util.List<com.company.officialwebsite.modules.content.entity.ContentReferenceEntity> findReferencesByReferrer(String referrerType, String referrerKey);

    void syncReferences(String referrerType, String referrerKey, java.util.List<com.company.officialwebsite.modules.content.entity.ContentReferenceEntity> newReferences);
}
