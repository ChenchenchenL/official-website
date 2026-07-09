package com.company.officialwebsite.modules.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.MediaAssetStatusEnum;
import com.company.officialwebsite.modules.business.entity.BusinessPageBlockEntity;
import com.company.officialwebsite.modules.business.entity.BusinessPageEntity;
import com.company.officialwebsite.modules.business.entity.BusinessRegistryEntity;
import com.company.officialwebsite.modules.business.mapper.BusinessPageBlockMapper;
import com.company.officialwebsite.modules.business.mapper.BusinessPageMapper;
import com.company.officialwebsite.modules.business.mapper.BusinessRegistryMapper;
import com.company.officialwebsite.modules.casecenter.entity.CaseEntity;
import com.company.officialwebsite.modules.casecenter.mapper.CaseMapper;
import com.company.officialwebsite.modules.content.entity.ContentReferenceEntity;
import com.company.officialwebsite.modules.content.mapper.ContentReferenceMapper;
import com.company.officialwebsite.modules.dashboard.service.AdminDashboardService;
import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardBusinessStatsVO;
import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardContentStatsVO;
import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardLeadStatsVO;
import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardMediaStatsVO;
import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardRiskAlertsVO;
import com.company.officialwebsite.modules.lead.entity.LeadEntity;
import com.company.officialwebsite.modules.lead.enums.LeadStatusEnum;
import com.company.officialwebsite.modules.lead.mapper.LeadMapper;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.mapper.MediaAssetMapper;
import com.company.officialwebsite.modules.product.entity.ProductEntity;
import com.company.officialwebsite.modules.product.mapper.ProductMapper;
import com.company.officialwebsite.modules.site.entity.AiCardEntity;
import com.company.officialwebsite.modules.site.mapper.AiCardMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_OFFLINE = "OFFLINE";

    private final ProductMapper productMapper;
    private final CaseMapper caseMapper;
    private final AiCardMapper aiCardMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final BusinessPageMapper businessPageMapper;
    private final BusinessPageBlockMapper businessPageBlockMapper;
    private final BusinessRegistryMapper businessRegistryMapper;
    private final ContentReferenceMapper contentReferenceMapper;
    private final LeadMapper leadMapper;

    public AdminDashboardServiceImpl(
            ProductMapper productMapper,
            CaseMapper caseMapper,
            AiCardMapper aiCardMapper,
            MediaAssetMapper mediaAssetMapper,
            BusinessPageMapper businessPageMapper,
            BusinessPageBlockMapper businessPageBlockMapper,
            BusinessRegistryMapper businessRegistryMapper,
            ContentReferenceMapper contentReferenceMapper,
            LeadMapper leadMapper) {
        this.productMapper = productMapper;
        this.caseMapper = caseMapper;
        this.aiCardMapper = aiCardMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.businessPageMapper = businessPageMapper;
        this.businessPageBlockMapper = businessPageBlockMapper;
        this.businessRegistryMapper = businessRegistryMapper;
        this.contentReferenceMapper = contentReferenceMapper;
        this.leadMapper = leadMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardContentStatsVO getContentStats() {
        AdminDashboardContentStatsVO vo = new AdminDashboardContentStatsVO();
        vo.setProductCount(countProducts());
        vo.setCaseCount(countCases());
        vo.setAiAbilityCount(countAiAbilities());
        vo.setMediaCount(countMediaAssets());
        vo.setPageCount(countBusinessPages());
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardLeadStatsVO getLeadStats() {
        AdminDashboardLeadStatsVO vo = new AdminDashboardLeadStatsVO();
        vo.setTotalCount(countLeads());
        vo.setCurrentMonthNewCount(countCurrentMonthNewLeads());
        vo.setPendingCount(countLeadsByStatus(LeadStatusEnum.UNHANDLED.getCode()));
        vo.setHandledCount(countHandledLeads());
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardMediaStatsVO getMediaStats() {
        AdminDashboardMediaStatsVO vo = new AdminDashboardMediaStatsVO();
        vo.setImageCount(countMediaAssetsByType("IMAGE"));
        vo.setVideoCount(countMediaAssetsByType("VIDEO"));
        vo.setDocumentCount(countMediaAssetsByType("DOCUMENT"));
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardBusinessStatsVO getBusinessStats() {
        AdminDashboardBusinessStatsVO vo = new AdminDashboardBusinessStatsVO();
        List<AdminDashboardBusinessStatsVO.BusinessModuleStatsVO> modules = new ArrayList<>();
        for (Map.Entry<String, String> entry : businessModuleNames().entrySet()) {
            modules.add(buildBusinessModuleStats(entry.getKey(), entry.getValue()));
        }
        vo.setModules(modules);
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardRiskAlertsVO getRiskAlerts() {
        AdminDashboardRiskAlertsVO vo = new AdminDashboardRiskAlertsVO();
        vo.setUnpublishedContentCount(countUnpublishedContent());
        vo.setInvalidContentCount(countInvalidContent());
        vo.setReferencedContentCount(countReferencedContent());
        vo.setPendingLeadCount(countLeadsByStatus(LeadStatusEnum.UNHANDLED.getCode()));
        return vo;
    }

    private Long countProducts() {
        return productMapper.selectCount(new LambdaQueryWrapper<ProductEntity>()
                .eq(ProductEntity::getDeletedMarker, 0L));
    }

    private Long countCases() {
        return caseMapper.selectCount(new LambdaQueryWrapper<CaseEntity>()
                .eq(CaseEntity::getDeletedMarker, 0L));
    }

    private Long countAiAbilities() {
        return aiCardMapper.selectCount(new LambdaQueryWrapper<AiCardEntity>()
                .eq(AiCardEntity::getDeletedMarker, 0L));
    }

    private Long countMediaAssets() {
        return mediaAssetMapper.selectCount(new LambdaQueryWrapper<MediaAssetEntity>()
                .eq(MediaAssetEntity::getDeletedMarker, 0L)
                .ne(MediaAssetEntity::getStatus, MediaAssetStatusEnum.DELETED.getCode()));
    }

    private Long countBusinessPages() {
        return businessPageMapper.selectCount(new LambdaQueryWrapper<BusinessPageEntity>()
                .eq(BusinessPageEntity::getDeletedMarker, 0L));
    }

    private Long countLeads() {
        return leadMapper.selectCount(new LambdaQueryWrapper<LeadEntity>()
                .eq(LeadEntity::getDeletedMarker, 0L));
    }

    private Long countCurrentMonthNewLeads() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);
        return leadMapper.selectCount(new LambdaQueryWrapper<LeadEntity>()
                .eq(LeadEntity::getDeletedMarker, 0L)
                .ge(LeadEntity::getCreatedAt, monthStart)
                .lt(LeadEntity::getCreatedAt, nextMonthStart));
    }

    private Long countLeadsByStatus(int status) {
        return leadMapper.selectCount(new LambdaQueryWrapper<LeadEntity>()
                .eq(LeadEntity::getDeletedMarker, 0L)
                .eq(LeadEntity::getStatus, status));
    }

    private Long countHandledLeads() {
        return leadMapper.selectCount(new LambdaQueryWrapper<LeadEntity>()
                .eq(LeadEntity::getDeletedMarker, 0L)
                .in(LeadEntity::getStatus, List.of(
                        LeadStatusEnum.PROCESSING.getCode(),
                        LeadStatusEnum.ARCHIVED.getCode())));
    }

    private Long countMediaAssetsByType(String mediaType) {
        return mediaAssetMapper.selectCount(new LambdaQueryWrapper<MediaAssetEntity>()
                .eq(MediaAssetEntity::getDeletedMarker, 0L)
                .ne(MediaAssetEntity::getStatus, MediaAssetStatusEnum.DELETED.getCode())
                .eq(MediaAssetEntity::getMediaType, mediaType));
    }

    private Long countUnpublishedContent() {
        return countProductsByStatus(STATUS_DRAFT)
                + countCasesByStatus(STATUS_DRAFT)
                + countBusinessesByStatus(STATUS_DRAFT)
                + countBusinessPagesByStatus(STATUS_DRAFT);
    }

    private Long countInvalidContent() {
        return countProductsByStatus(STATUS_OFFLINE)
                + countCasesByStatus(STATUS_OFFLINE)
                + countBusinessesByStatus(STATUS_OFFLINE)
                + countBusinessPagesByStatus(STATUS_OFFLINE);
    }

    private Long countProductsByStatus(String status) {
        return productMapper.selectCount(new LambdaQueryWrapper<ProductEntity>()
                .eq(ProductEntity::getDeletedMarker, 0L)
                .eq(ProductEntity::getStatus, status));
    }

    private Long countCasesByStatus(String status) {
        return caseMapper.selectCount(new LambdaQueryWrapper<CaseEntity>()
                .eq(CaseEntity::getDeletedMarker, 0L)
                .eq(CaseEntity::getStatus, status));
    }

    private Long countBusinessesByStatus(String status) {
        return businessRegistryMapper.selectCount(new LambdaQueryWrapper<BusinessRegistryEntity>()
                .eq(BusinessRegistryEntity::getDeletedMarker, 0L)
                .eq(BusinessRegistryEntity::getBusinessStatus, status));
    }

    private Long countBusinessPagesByStatus(String status) {
        return businessPageMapper.selectCount(new LambdaQueryWrapper<BusinessPageEntity>()
                .eq(BusinessPageEntity::getDeletedMarker, 0L)
                .eq(BusinessPageEntity::getPageStatus, status));
    }

    private Long countReferencedContent() {
        return (long) contentReferenceMapper.selectList(new LambdaQueryWrapper<ContentReferenceEntity>()
                .select(ContentReferenceEntity::getReferencedType, ContentReferenceEntity::getReferencedId)
                .eq(ContentReferenceEntity::getDeletedMarker, 0L)
                .groupBy(ContentReferenceEntity::getReferencedType, ContentReferenceEntity::getReferencedId))
                .size();
    }

    private AdminDashboardBusinessStatsVO.BusinessModuleStatsVO buildBusinessModuleStats(
            String businessCode, String fallbackName) {
        BusinessRegistryEntity business = businessRegistryMapper.selectOne(
                new LambdaQueryWrapper<BusinessRegistryEntity>()
                        .eq(BusinessRegistryEntity::getDeletedMarker, 0L)
                        .eq(BusinessRegistryEntity::getBusinessCode, businessCode)
                        .last("limit 1"));
        AdminDashboardBusinessStatsVO.BusinessModuleStatsVO vo =
                new AdminDashboardBusinessStatsVO.BusinessModuleStatsVO();
        vo.setBusinessCode(businessCode);
        vo.setBusinessName(business == null ? fallbackName : business.getBusinessName());
        vo.setBusinessStatus(business == null ? "UNCONFIGURED" : business.getBusinessStatus());
        Long pageCount = business == null ? 0L : countBusinessPagesByBusinessId(business.getId());
        Long pageBlockCount = business == null ? 0L : countBusinessPageBlocksByBusinessId(business.getId());
        vo.setPageCount(pageCount);
        vo.setPageBlockCount(pageBlockCount);
        vo.setContentCount(pageCount + pageBlockCount);
        return vo;
    }

    private Long countBusinessPagesByBusinessId(Long businessId) {
        return businessPageMapper.selectCount(new LambdaQueryWrapper<BusinessPageEntity>()
                .eq(BusinessPageEntity::getDeletedMarker, 0L)
                .eq(BusinessPageEntity::getBusinessId, businessId));
    }

    private Long countBusinessPageBlocksByBusinessId(Long businessId) {
        List<Long> pageIds = businessPageMapper.selectList(new LambdaQueryWrapper<BusinessPageEntity>()
                        .eq(BusinessPageEntity::getDeletedMarker, 0L)
                        .eq(BusinessPageEntity::getBusinessId, businessId))
                .stream()
                .map(BusinessPageEntity::getId)
                .toList();
        if (pageIds.isEmpty()) {
            return 0L;
        }
        return businessPageBlockMapper.selectCount(new LambdaQueryWrapper<BusinessPageBlockEntity>()
                .eq(BusinessPageBlockEntity::getDeletedMarker, 0L)
                .in(BusinessPageBlockEntity::getPageId, pageIds));
    }

    private Map<String, String> businessModuleNames() {
        Map<String, String> modules = new LinkedHashMap<>();
        modules.put("AI_CENTER", "AI能力中心");
        modules.put("MEDICAL", "医疗");
        modules.put("EDUCATION", "教育");
        modules.put("INDUSTRY", "工业");
        modules.put("TONGYUN", "砼云");
        return modules;
    }
}
