package com.company.officialwebsite.modules.lead.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.lead.dto.ContactInfoUpdateRequestDTO;
import com.company.officialwebsite.modules.lead.entity.ContactInfoEntity;
import com.company.officialwebsite.modules.lead.mapper.ContactInfoMapper;
import com.company.officialwebsite.modules.lead.service.ContactInfoModuleConstants;
import com.company.officialwebsite.modules.lead.service.ContactInfoService;
import com.company.officialwebsite.modules.lead.vo.AdminContactInfoVO;
import com.company.officialwebsite.modules.lead.vo.PortalContactInfoVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import com.company.officialwebsite.infrastructure.event.EntityChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ContactInfoServiceImpl：实现基础联系方式的后台单例维护、前台缓存读取、审计和缓存失效逻辑。
 */
@Service
public class ContactInfoServiceImpl implements ContactInfoService {

    private static final Logger log = LoggerFactory.getLogger(ContactInfoServiceImpl.class);

    private static final String BIZ_MODULE = "LEAD";
    private static final String TARGET_TYPE = "CONTACT_INFO";
    private static final String ACTION_UPDATE = "UPDATE_CONTACT_INFO";

    private static final String MSG_ADDRESS_REQUIRED = "联系地址不能为空";
    private static final String MSG_PHONE_REQUIRED = "商务咨询电话不能为空";
    private static final String MSG_PHONE_INVALID = "商务咨询电话包含非法字符";
    private static final String MSG_PHONE_NO_DIGIT = "商务咨询电话必须至少包含一个数字";
    private static final String MSG_EMAIL_REQUIRED = "联系邮箱不能为空";
    private static final String MSG_EMAIL_INVALID = "联系邮箱格式不合法";

    /**
     * 电话允许字符：数字、空格、+、-、(、)、/、#。
     */
    private static final Pattern PHONE_ALLOWED_CHARS = Pattern.compile("^[0-9 +\\-()/#]+$");

    /**
     * 至少包含一个数字。
     */
    private static final Pattern PHONE_CONTAINS_DIGIT = Pattern.compile("\\d");

    private final ContactInfoMapper contactInfoMapper;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final ApplicationEventPublisher eventPublisher;

    public ContactInfoServiceImpl(
            ContactInfoMapper contactInfoMapper,
            AuditLogService auditLogService,
            PortalCacheSupport portalCacheSupport,
            ApplicationEventPublisher eventPublisher) {
        this.contactInfoMapper = contactInfoMapper;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminContactInfoVO getAdminContactInfo() {
        ContactInfoEntity entity = requireConfig();
        return toAdminVO(entity);
    }

    @Override
    @Transactional
    public void updateContactInfo(ContactInfoUpdateRequestDTO requestDTO) {
        ContactInfoEntity entity = requireConfig();
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);

        String normalizedAddress = normalizeAddress(requestDTO.getContactAddress());
        String normalizedPhone = normalizePhone(requestDTO.getBusinessPhone());
        String normalizedEmail = normalizeEmail(requestDTO.getContactEmail());

        entity.setContactAddress(normalizedAddress);
        entity.setBusinessPhone(normalizedPhone);
        entity.setContactEmail(normalizedEmail);

        ConcurrencyHelper.tryUpdate(contactInfoMapper, entity);
        log.info("update contact info success id={} previousVersion={} currentVersion={}",
                entity.getId(), requestDTO.getVersion(), entity.getVersion());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalCache();
        eventPublisher.publishEvent(EntityChangedEvent.of(this, "lead", "ContactInfo", "default"));
    }

    @Override
    @Transactional(readOnly = true)
    public PortalContactInfoVO getPortalContactInfo() {
        String cacheKey = portalCacheSupport.buildKey(ContactInfoModuleConstants.CACHE_SEGMENT);
        PortalContactInfoVO cached = portalCacheSupport.readCache(cacheKey, PortalContactInfoVO.class, ContactInfoModuleConstants.CACHE_SEGMENT);
        if (cached != null) {
            return cached;
        }

        ContactInfoEntity entity = requireConfig();
        PortalContactInfoVO vo = toPortalVO(entity);
        portalCacheSupport.writeCache(cacheKey, vo, portalCacheSupport.isEmptyResult(vo), ContactInfoModuleConstants.CACHE_SEGMENT);
        return vo;
    }

    private ContactInfoEntity requireConfig() {
        ContactInfoEntity entity = contactInfoMapper.selectOne(
                new LambdaQueryWrapper<ContactInfoEntity>()
                        .eq(ContactInfoEntity::getConfigKey, ContactInfoModuleConstants.CONFIG_KEY)
                        .eq(ContactInfoEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            log.warn("contact info config not found configKey={}", ContactInfoModuleConstants.CONFIG_KEY);
            throw new BusinessException(ErrorCode.LEAD_CONTACT_INFO_NOT_FOUND);
        }
        return entity;
    }

    private String normalizeAddress(String address) {
        String normalized = StringFieldUtils.trimToNull(address);
        if (normalized == null) {
            log.warn("contact address blank after trim");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_ADDRESS_REQUIRED);
        }
        return normalized;
    }

    private String normalizePhone(String phone) {
        String normalized = StringFieldUtils.trimToNull(phone);
        if (normalized == null) {
            log.warn("business phone blank after trim");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_PHONE_REQUIRED);
        }
        if (!PHONE_ALLOWED_CHARS.matcher(normalized).matches()) {
            log.warn("business phone contains illegal chars phone={}", normalized);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_PHONE_INVALID);
        }
        if (!PHONE_CONTAINS_DIGIT.matcher(normalized).find()) {
            log.warn("business phone contains no digit phone={}", normalized);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_PHONE_NO_DIGIT);
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = StringFieldUtils.trimToNull(email);
        if (normalized == null) {
            log.warn("contact email blank after trim");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EMAIL_REQUIRED);
        }
        if (!isValidEmail(normalized)) {
            log.warn("contact email invalid format email={}", normalized);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EMAIL_INVALID);
        }
        return normalized;
    }

    private boolean isValidEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex >= email.length() - 1) {
            return false;
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);
        if (localPart.isEmpty() || localPart.length() > 64) {
            return false;
        }
        int dotIndex = domainPart.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex >= domainPart.length() - 1) {
            return false;
        }
        return true;
    }

    private AdminContactInfoVO toAdminVO(ContactInfoEntity entity) {
        AdminContactInfoVO vo = new AdminContactInfoVO();
        vo.setId(entity.getId());
        vo.setContactAddress(StringFieldUtils.defaultString(entity.getContactAddress()));
        vo.setBusinessPhone(StringFieldUtils.defaultString(entity.getBusinessPhone()));
        vo.setContactEmail(StringFieldUtils.defaultString(entity.getContactEmail()));
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private PortalContactInfoVO toPortalVO(ContactInfoEntity entity) {
        PortalContactInfoVO vo = new PortalContactInfoVO();
        vo.setContactAddress(StringFieldUtils.defaultString(entity.getContactAddress()));
        vo.setBusinessPhone(StringFieldUtils.defaultString(entity.getBusinessPhone()));
        vo.setContactEmail(StringFieldUtils.defaultString(entity.getContactEmail()));
        return vo;
    }

    private Map<String, Object> toSnapshot(ContactInfoEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("configKey", entity.getConfigKey());
        snapshot.put("contactAddress", entity.getContactAddress());
        snapshot.put("businessPhone", entity.getBusinessPhone());
        snapshot.put("contactEmail", entity.getContactEmail());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private void invalidatePortalCache() {
        portalCacheSupport.invalidatePortalKey(ContactInfoModuleConstants.CACHE_SEGMENT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
