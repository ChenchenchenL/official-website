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
import com.company.officialwebsite.modules.content.dto.ContentRelationCreateRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentRelationUpdateRequestDTO;
import com.company.officialwebsite.modules.content.entity.ContentRelationEntity;
import com.company.officialwebsite.modules.content.mapper.ContentRelationMapper;
import com.company.officialwebsite.modules.content.service.ContentRelationService;
import com.company.officialwebsite.modules.content.vo.AdminContentRelationVO;
import com.company.officialwebsite.modules.product.entity.IndustrySolutionEntity;
import com.company.officialwebsite.modules.product.entity.ProductEntity;
import com.company.officialwebsite.modules.product.mapper.IndustrySolutionMapper;
import com.company.officialwebsite.modules.product.mapper.ProductMapper;
import com.company.officialwebsite.modules.site.entity.AiCardEntity;
import com.company.officialwebsite.modules.site.entity.ResearchDirectionEntity;
import com.company.officialwebsite.modules.site.mapper.AiCardMapper;
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
public class ContentRelationServiceImpl implements ContentRelationService {

    private static final Logger log = LoggerFactory.getLogger(ContentRelationServiceImpl.class);

    private static final String BIZ_MODULE = "CONTENT";
    private static final String TARGET_TYPE = "CONTENT_RELATION";
    private static final String ACTION_CREATE = "CREATE_CONTENT_RELATION";
    private static final String ACTION_UPDATE = "UPDATE_CONTENT_RELATION";
    private static final String ACTION_DELETE = "DELETE_CONTENT_RELATION";
    private static final String TYPE_PRODUCT = "PRODUCT";
    private static final String TYPE_CASE = "CASE";
    private static final String TYPE_INDUSTRY_SOLUTION = "INDUSTRY_SOLUTION";
    private static final String TYPE_AI_CARD = "AI_CARD";
    private static final String TYPE_RESEARCH_DIRECTION = "RESEARCH_DIRECTION";
    private static final String REL_PRODUCT_CASE = "PRODUCT_CASE";
    private static final String REL_CASE_INDUSTRY = "CASE_INDUSTRY";
    private static final String REL_AI_PRODUCT = "AI_PRODUCT";
    private static final String REL_RESEARCH_PRODUCT = "RESEARCH_PRODUCT";
    private static final String MSG_NOT_FOUND = "Content relation does not exist or has been deleted";
    private static final String MSG_DUPLICATE = "Content relation already exists";
    private static final String MSG_SOURCE_TYPE_REQUIRED = "Source type cannot be empty";
    private static final String MSG_TARGET_TYPE_REQUIRED = "Target type cannot be empty";
    private static final String MSG_RELATION_TYPE_REQUIRED = "Relation type cannot be empty";
    private static final String MSG_RELATION_COMBINATION_INVALID = "Relation type does not match source and target type";
    private static final String MSG_SOURCE_NOT_FOUND = "Source content does not exist or has been deleted";
    private static final String MSG_TARGET_NOT_FOUND = "Target content does not exist or has been deleted";

    private final ContentRelationMapper contentRelationMapper;
    private final ProductMapper productMapper;
    private final CaseMapper caseMapper;
    private final IndustrySolutionMapper industrySolutionMapper;
    private final AiCardMapper aiCardMapper;
    private final ResearchDirectionMapper researchDirectionMapper;
    private final AuditLogService auditLogService;

    public ContentRelationServiceImpl(
            ContentRelationMapper contentRelationMapper,
            ProductMapper productMapper,
            CaseMapper caseMapper,
            IndustrySolutionMapper industrySolutionMapper,
            AiCardMapper aiCardMapper,
            ResearchDirectionMapper researchDirectionMapper,
            AuditLogService auditLogService) {
        this.contentRelationMapper = contentRelationMapper;
        this.productMapper = productMapper;
        this.caseMapper = caseMapper;
        this.industrySolutionMapper = industrySolutionMapper;
        this.aiCardMapper = aiCardMapper;
        this.researchDirectionMapper = researchDirectionMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminContentRelationVO> getAdminContentRelationList(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        Page<ContentRelationEntity> page = contentRelationMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<ContentRelationEntity>()
                        .eq(ContentRelationEntity::getDeletedMarker, 0L)
                        .orderByAsc(ContentRelationEntity::getSourceType)
                        .orderByAsc(ContentRelationEntity::getSourceId)
                        .orderByAsc(ContentRelationEntity::getRelationType)
                        .orderByAsc(ContentRelationEntity::getId));
        List<AdminContentRelationVO> list = page.getRecords().stream()
                .map(this::toAdminVO)
                .toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public void createContentRelation(ContentRelationCreateRequestDTO requestDTO) {
        ContentRelationEntity entity = new ContentRelationEntity();
        applyRequest(entity, requestDTO.getSourceType(), requestDTO.getSourceId(), requestDTO.getTargetType(),
                requestDTO.getTargetId(), requestDTO.getRelationType());
        validateRelation(entity);

        try {
            contentRelationMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create content relation duplicate sourceType={} sourceId={} targetType={} targetId={} relationType={}",
                    entity.getSourceType(), entity.getSourceId(), entity.getTargetType(), entity.getTargetId(),
                    entity.getRelationType(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }

        log.info("create content relation success id={} relationType={}", entity.getId(), entity.getRelationType());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void updateContentRelation(Long id, ContentRelationUpdateRequestDTO requestDTO) {
        ContentRelationEntity entity = requireActiveRelation(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);

        applyRequest(entity, requestDTO.getSourceType(), requestDTO.getSourceId(), requestDTO.getTargetType(),
                requestDTO.getTargetId(), requestDTO.getRelationType());
        validateRelation(entity);

        try {
            ConcurrencyHelper.tryUpdate(contentRelationMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update content relation duplicate id={} sourceType={} sourceId={} targetType={} targetId={} relationType={}",
                    entity.getId(), entity.getSourceType(), entity.getSourceId(), entity.getTargetType(),
                    entity.getTargetId(), entity.getRelationType(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }

        log.info("update content relation success id={} relationType={}", entity.getId(), entity.getRelationType());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void deleteContentRelation(Long id, Integer version) {
        ContentRelationEntity entity = requireActiveRelation(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        int deleted = contentRelationMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }
        log.info("delete content relation success id={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
    }

    private ContentRelationEntity requireActiveRelation(Long id) {
        ContentRelationEntity entity = contentRelationMapper.selectOne(
                new LambdaQueryWrapper<ContentRelationEntity>()
                        .eq(ContentRelationEntity::getId, id)
                        .eq(ContentRelationEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            log.warn("content relation not found id={}", id);
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_NOT_FOUND);
        }
        return entity;
    }

    private void applyRequest(
            ContentRelationEntity entity,
            String sourceType,
            Long sourceId,
            String targetType,
            Long targetId,
            String relationType) {
        entity.setSourceType(normalizeCode(sourceType, MSG_SOURCE_TYPE_REQUIRED));
        entity.setSourceId(sourceId);
        entity.setTargetType(normalizeCode(targetType, MSG_TARGET_TYPE_REQUIRED));
        entity.setTargetId(targetId);
        entity.setRelationType(normalizeCode(relationType, MSG_RELATION_TYPE_REQUIRED));
    }

    private void validateRelation(ContentRelationEntity entity) {
        if (!isAllowedCombination(entity)) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_RELATION_COMBINATION_INVALID);
        }
        if (!contentExists(entity.getSourceType(), entity.getSourceId())) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_SOURCE_NOT_FOUND);
        }
        if (!contentExists(entity.getTargetType(), entity.getTargetId())) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_TARGET_NOT_FOUND);
        }
    }

    private boolean isAllowedCombination(ContentRelationEntity entity) {
        return (REL_PRODUCT_CASE.equals(entity.getRelationType())
                        && TYPE_PRODUCT.equals(entity.getSourceType())
                        && TYPE_CASE.equals(entity.getTargetType()))
                || (REL_CASE_INDUSTRY.equals(entity.getRelationType())
                        && TYPE_CASE.equals(entity.getSourceType())
                        && TYPE_INDUSTRY_SOLUTION.equals(entity.getTargetType()))
                || (REL_AI_PRODUCT.equals(entity.getRelationType())
                        && TYPE_AI_CARD.equals(entity.getSourceType())
                        && TYPE_PRODUCT.equals(entity.getTargetType()))
                || (REL_RESEARCH_PRODUCT.equals(entity.getRelationType())
                        && TYPE_RESEARCH_DIRECTION.equals(entity.getSourceType())
                        && TYPE_PRODUCT.equals(entity.getTargetType()));
    }

    private boolean contentExists(String contentType, Long id) {
        if (id == null) {
            return false;
        }
        return switch (contentType) {
            case TYPE_PRODUCT -> exists(productMapper, id);
            case TYPE_CASE -> exists(caseMapper, id);
            case TYPE_INDUSTRY_SOLUTION -> exists(industrySolutionMapper, id);
            case TYPE_AI_CARD -> exists(aiCardMapper, id);
            case TYPE_RESEARCH_DIRECTION -> exists(researchDirectionMapper, id);
            default -> false;
        };
    }

    private <T extends BaseEntity> boolean exists(BaseMapper<T> mapper, Long id) {
        Long count = mapper.selectCount(
                new QueryWrapper<T>()
                        .eq("id", id)
                        .eq("deleted_marker", 0L));
        return count != null && count > 0;
    }

    private AdminContentRelationVO toAdminVO(ContentRelationEntity entity) {
        AdminContentRelationVO vo = new AdminContentRelationVO();
        vo.setId(entity.getId());
        vo.setSourceType(StringFieldUtils.defaultString(entity.getSourceType()));
        vo.setSourceId(entity.getSourceId());
        vo.setTargetType(StringFieldUtils.defaultString(entity.getTargetType()));
        vo.setTargetId(entity.getTargetId());
        vo.setRelationType(StringFieldUtils.defaultString(entity.getRelationType()));
        vo.setRelationName(relationName(entity.getRelationType()));
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private Map<String, Object> toSnapshot(ContentRelationEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("sourceType", entity.getSourceType());
        snapshot.put("sourceId", entity.getSourceId());
        snapshot.put("targetType", entity.getTargetType());
        snapshot.put("targetId", entity.getTargetId());
        snapshot.put("relationType", entity.getRelationType());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private String relationName(String relationType) {
        return switch (StringFieldUtils.defaultString(relationType)) {
            case REL_PRODUCT_CASE -> "产品 -> 案例";
            case REL_CASE_INDUSTRY -> "案例 -> 行业方案";
            case REL_AI_PRODUCT -> "AI能力 -> 产品";
            case REL_RESEARCH_PRODUCT -> "研发方向 -> 产品";
            default -> relationType;
        };
    }

    private String normalizeCode(String value, String blankMessage) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, blankMessage);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
