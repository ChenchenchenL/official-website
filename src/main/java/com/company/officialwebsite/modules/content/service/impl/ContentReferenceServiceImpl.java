package com.company.officialwebsite.modules.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.entity.BaseEntity;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.casecenter.entity.CaseEntity;
import com.company.officialwebsite.modules.casecenter.mapper.CaseMapper;
import com.company.officialwebsite.modules.content.dto.ContentReferenceCreateRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentReferenceUpdateRequestDTO;
import com.company.officialwebsite.modules.content.entity.ContentReferenceEntity;
import com.company.officialwebsite.modules.content.mapper.ContentReferenceMapper;
import com.company.officialwebsite.modules.content.service.ContentReferenceService;
import com.company.officialwebsite.modules.content.vo.AdminContentReferenceVO;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.mapper.MediaAssetMapper;
import com.company.officialwebsite.modules.product.entity.IndustrySolutionEntity;
import com.company.officialwebsite.modules.product.entity.ProductEntity;
import com.company.officialwebsite.modules.product.mapper.IndustrySolutionMapper;
import com.company.officialwebsite.modules.product.mapper.ProductMapper;
import com.company.officialwebsite.modules.site.entity.AiCardEntity;
import com.company.officialwebsite.modules.site.entity.ClientLogoEntity;
import com.company.officialwebsite.modules.site.entity.ResearchDirectionEntity;
import com.company.officialwebsite.modules.site.mapper.AiCardMapper;
import com.company.officialwebsite.modules.site.mapper.ClientLogoMapper;
import com.company.officialwebsite.modules.site.mapper.ResearchDirectionMapper;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentReferenceServiceImpl implements ContentReferenceService {

    private static final Logger log = LoggerFactory.getLogger(ContentReferenceServiceImpl.class);

    private static final String BIZ_MODULE = "CONTENT";
    private static final String TARGET_TYPE = "CONTENT_REFERENCE";
    private static final String ACTION_CREATE = "CREATE_CONTENT_REFERENCE";
    private static final String ACTION_UPDATE = "UPDATE_CONTENT_REFERENCE";
    private static final String ACTION_DELETE = "DELETE_CONTENT_REFERENCE";
    private static final String TYPE_PRODUCT = "PRODUCT";
    private static final String TYPE_CASE = "CASE";
    private static final String TYPE_INDUSTRY_SOLUTION = "INDUSTRY_SOLUTION";
    private static final String TYPE_AI_CARD = "AI_CARD";
    private static final String TYPE_RESEARCH_DIRECTION = "RESEARCH_DIRECTION";
    private static final String TYPE_CLIENT_LOGO = "CLIENT_LOGO";
    private static final String TYPE_MEDIA_ASSET = "MEDIA_ASSET";
    private static final String MSG_NOT_FOUND = "Content reference does not exist or has been deleted";
    private static final String MSG_DUPLICATE = "Content reference already exists";
    private static final String MSG_REFERRER_TYPE_REQUIRED = "Referrer type cannot be empty";
    private static final String MSG_REFERRER_KEY_REQUIRED = "Referrer key cannot be empty";
    private static final String MSG_REFERENCED_TYPE_REQUIRED = "Referenced type cannot be empty";
    private static final String MSG_REFERENCE_TYPE_REQUIRED = "Reference type cannot be empty";
    private static final String MSG_REFERENCED_TYPE_INVALID = "Referenced type is not supported";
    private static final String MSG_REFERENCED_NOT_FOUND = "Referenced content does not exist or has been deleted";

    private final ContentReferenceMapper contentReferenceMapper;
    private final ProductMapper productMapper;
    private final CaseMapper caseMapper;
    private final IndustrySolutionMapper industrySolutionMapper;
    private final AiCardMapper aiCardMapper;
    private final ResearchDirectionMapper researchDirectionMapper;
    private final ClientLogoMapper clientLogoMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final AuditLogService auditLogService;

    public ContentReferenceServiceImpl(
            ContentReferenceMapper contentReferenceMapper,
            ProductMapper productMapper,
            CaseMapper caseMapper,
            IndustrySolutionMapper industrySolutionMapper,
            AiCardMapper aiCardMapper,
            ResearchDirectionMapper researchDirectionMapper,
            ClientLogoMapper clientLogoMapper,
            MediaAssetMapper mediaAssetMapper,
            AuditLogService auditLogService) {
        this.contentReferenceMapper = contentReferenceMapper;
        this.productMapper = productMapper;
        this.caseMapper = caseMapper;
        this.industrySolutionMapper = industrySolutionMapper;
        this.aiCardMapper = aiCardMapper;
        this.researchDirectionMapper = researchDirectionMapper;
        this.clientLogoMapper = clientLogoMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminContentReferenceVO> getAdminContentReferenceList(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        Page<ContentReferenceEntity> page = contentReferenceMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<ContentReferenceEntity>()
                        .eq(ContentReferenceEntity::getDeletedMarker, 0L)
                        .orderByAsc(ContentReferenceEntity::getReferrerType)
                        .orderByAsc(ContentReferenceEntity::getReferrerKey)
                        .orderByAsc(ContentReferenceEntity::getReferenceType)
                        .orderByAsc(ContentReferenceEntity::getId));
        List<AdminContentReferenceVO> list = page.getRecords().stream()
                .map(this::toAdminVO)
                .toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public void createContentReference(ContentReferenceCreateRequestDTO requestDTO) {
        ContentReferenceEntity entity = new ContentReferenceEntity();
        applyRequest(entity, requestDTO.getReferrerType(), requestDTO.getReferrerKey(),
                requestDTO.getReferencedType(), requestDTO.getReferencedId(), requestDTO.getReferenceType());
        validateReferencedContent(entity);

        try {
            contentReferenceMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create content reference duplicate referrerType={} referrerKey={} referencedType={} referencedId={} referenceType={}",
                    entity.getReferrerType(), entity.getReferrerKey(), entity.getReferencedType(),
                    entity.getReferencedId(), entity.getReferenceType(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }

        log.info("create content reference success id={} referenceType={}", entity.getId(), entity.getReferenceType());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void updateContentReference(Long id, ContentReferenceUpdateRequestDTO requestDTO) {
        ContentReferenceEntity entity = requireActiveReference(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);

        applyRequest(entity, requestDTO.getReferrerType(), requestDTO.getReferrerKey(),
                requestDTO.getReferencedType(), requestDTO.getReferencedId(), requestDTO.getReferenceType());
        validateReferencedContent(entity);

        try {
            ConcurrencyHelper.tryUpdate(contentReferenceMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update content reference duplicate id={} referrerType={} referrerKey={} referencedType={} referencedId={} referenceType={}",
                    entity.getId(), entity.getReferrerType(), entity.getReferrerKey(), entity.getReferencedType(),
                    entity.getReferencedId(), entity.getReferenceType(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }

        log.info("update content reference success id={} referenceType={}", entity.getId(), entity.getReferenceType());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void deleteContentReference(Long id, Integer version) {
        ContentReferenceEntity entity = requireActiveReference(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        int deleted = contentReferenceMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }
        log.info("delete content reference success id={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveReferences(String referencedType, Long referencedId) {
        if (!org.springframework.util.StringUtils.hasText(referencedType) || referencedId == null) {
            return false;
        }
        Long count = contentReferenceMapper.selectCount(
                new LambdaQueryWrapper<ContentReferenceEntity>()
                        .eq(ContentReferenceEntity::getDeletedMarker, 0L)
                        .eq(ContentReferenceEntity::getReferencedType, referencedType.trim().toUpperCase(Locale.ROOT))
                        .eq(ContentReferenceEntity::getReferencedId, referencedId));
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentReferenceEntity> findReferencesByReferrer(String referrerType, String referrerKey) {
        if (!org.springframework.util.StringUtils.hasText(referrerType) || !org.springframework.util.StringUtils.hasText(referrerKey)) {
            return List.of();
        }
        return contentReferenceMapper.selectList(
                new LambdaQueryWrapper<ContentReferenceEntity>()
                        .eq(ContentReferenceEntity::getDeletedMarker, 0L)
                        .eq(ContentReferenceEntity::getReferrerType, referrerType.trim().toUpperCase(Locale.ROOT))
                        .eq(ContentReferenceEntity::getReferrerKey, referrerKey.trim()));
    }

    @Override
    @Transactional
    public void syncReferences(String referrerType, String referrerKey, List<ContentReferenceEntity> newReferences) {
        String normalizedReferrerType = normalizeCode(referrerType, MSG_REFERRER_TYPE_REQUIRED);
        String normalizedReferrerKey = normalizeText(referrerKey, MSG_REFERRER_KEY_REQUIRED);

        // 删除旧活跃引用
        contentReferenceMapper.delete(
                new LambdaQueryWrapper<ContentReferenceEntity>()
                        .eq(ContentReferenceEntity::getDeletedMarker, 0L)
                        .eq(ContentReferenceEntity::getReferrerType, normalizedReferrerType)
                        .eq(ContentReferenceEntity::getReferrerKey, normalizedReferrerKey));

        if (newReferences == null || newReferences.isEmpty()) {
            return;
        }

        for (ContentReferenceEntity ref : newReferences) {
            ref.setId(null);
            ref.setReferrerType(normalizedReferrerType);
            ref.setReferrerKey(normalizedReferrerKey);
            ref.setVersion(0);
            validateReferencedContent(ref);
            try {
                contentReferenceMapper.insert(ref);
            } catch (DuplicateKeyException ex) {
                log.warn("syncReferences duplicate referrerType={} referrerKey={} referencedType={} referencedId={}",
                        normalizedReferrerType, normalizedReferrerKey, ref.getReferencedType(), ref.getReferencedId());
            }
        }
    }

    private ContentReferenceEntity requireActiveReference(Long id) {
        ContentReferenceEntity entity = contentReferenceMapper.selectOne(
                new LambdaQueryWrapper<ContentReferenceEntity>()
                        .eq(ContentReferenceEntity::getId, id)
                        .eq(ContentReferenceEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            log.warn("content reference not found id={}", id);
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_NOT_FOUND);
        }
        return entity;
    }

    private void applyRequest(
            ContentReferenceEntity entity,
            String referrerType,
            String referrerKey,
            String referencedType,
            Long referencedId,
            String referenceType) {
        entity.setReferrerType(normalizeCode(referrerType, MSG_REFERRER_TYPE_REQUIRED));
        entity.setReferrerKey(normalizeText(referrerKey, MSG_REFERRER_KEY_REQUIRED));
        entity.setReferencedType(normalizeCode(referencedType, MSG_REFERENCED_TYPE_REQUIRED));
        entity.setReferencedId(referencedId);
        entity.setReferenceType(normalizeCode(referenceType, MSG_REFERENCE_TYPE_REQUIRED));
    }

    private void validateReferencedContent(ContentReferenceEntity entity) {
        if (!referencedContentExists(entity.getReferencedType(), entity.getReferencedId())) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_REFERENCED_NOT_FOUND);
        }
    }

    private boolean referencedContentExists(String referencedType, Long referencedId) {
        if (referencedId == null) {
            return false;
        }
        return switch (referencedType) {
            case TYPE_PRODUCT -> exists(productMapper, referencedId);
            case TYPE_CASE -> exists(caseMapper, referencedId);
            case TYPE_INDUSTRY_SOLUTION -> exists(industrySolutionMapper, referencedId);
            case TYPE_AI_CARD -> exists(aiCardMapper, referencedId);
            case TYPE_RESEARCH_DIRECTION -> exists(researchDirectionMapper, referencedId);
            case TYPE_CLIENT_LOGO -> exists(clientLogoMapper, referencedId);
            case TYPE_MEDIA_ASSET -> exists(mediaAssetMapper, referencedId);
            default -> throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_REFERENCED_TYPE_INVALID);
        };
    }

    private <T extends BaseEntity> boolean exists(BaseMapper<T> mapper, Long id) {
        Long count = mapper.selectCount(
                new QueryWrapper<T>()
                        .eq("id", id)
                        .eq("deleted_marker", 0L));
        return count != null && count > 0;
    }

    private AdminContentReferenceVO toAdminVO(ContentReferenceEntity entity) {
        AdminContentReferenceVO vo = new AdminContentReferenceVO();
        vo.setId(entity.getId());
        vo.setReferrerType(StringFieldUtils.defaultString(entity.getReferrerType()));
        vo.setReferrerKey(StringFieldUtils.defaultString(entity.getReferrerKey()));
        vo.setReferencedType(StringFieldUtils.defaultString(entity.getReferencedType()));
        vo.setReferencedId(entity.getReferencedId());
        vo.setReferenceType(StringFieldUtils.defaultString(entity.getReferenceType()));
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private Map<String, Object> toSnapshot(ContentReferenceEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("referrerType", entity.getReferrerType());
        snapshot.put("referrerKey", entity.getReferrerKey());
        snapshot.put("referencedType", entity.getReferencedType());
        snapshot.put("referencedId", entity.getReferencedId());
        snapshot.put("referenceType", entity.getReferenceType());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private String normalizeCode(String value, String blankMessage) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, blankMessage);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value, String blankMessage) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, blankMessage);
        }
        return normalized;
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
