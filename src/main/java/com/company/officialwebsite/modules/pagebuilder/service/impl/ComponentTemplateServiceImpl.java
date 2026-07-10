package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.pagebuilder.converter.ComponentTemplateConverter;
import com.company.officialwebsite.modules.pagebuilder.entity.ComponentTemplateEntity;
import com.company.officialwebsite.modules.pagebuilder.enums.ComponentTemplateStatusEnum;
import com.company.officialwebsite.modules.pagebuilder.mapper.ComponentTemplateMapper;
import com.company.officialwebsite.modules.pagebuilder.service.ComponentTemplateService;
import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ComponentTemplateServiceImpl: 组件物料模板管理服务实现类。
 */
@Service
public class ComponentTemplateServiceImpl implements ComponentTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ComponentTemplateServiceImpl.class);

    private final ComponentTemplateMapper templateMapper;
    private final ComponentTemplateConverter templateConverter;

    public ComponentTemplateServiceImpl(ComponentTemplateMapper templateMapper, ComponentTemplateConverter templateConverter) {
        this.templateMapper = templateMapper;
        this.templateConverter = templateConverter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComponentTemplateVO> getActiveTemplates() {
        List<ComponentTemplateEntity> entities = templateMapper.selectList(
                new LambdaQueryWrapper<ComponentTemplateEntity>()
                        .eq(ComponentTemplateEntity::getStatus, ComponentTemplateStatusEnum.ACTIVE.name())
                        .orderByAsc(ComponentTemplateEntity::getSortOrder)
                        .orderByAsc(ComponentTemplateEntity::getId)
        );
        log.info("query active component templates count={}", entities.size());
        return templateConverter.toVOList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public ComponentTemplateVO getTemplateByCode(String componentCode) {
        ComponentTemplateEntity entity = templateMapper.selectOne(
                new LambdaQueryWrapper<ComponentTemplateEntity>()
                        .eq(ComponentTemplateEntity::getComponentCode, componentCode)
                        .eq(ComponentTemplateEntity::getStatus, ComponentTemplateStatusEnum.ACTIVE.name())
        );
        if (entity == null) {
            log.warn("component template not found or inactive code={}", componentCode);
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "指定的组件模板不存在或已被禁用");
        }
        log.info("query component template detail success code={}", componentCode);
        return templateConverter.toVO(entity);
    }
}
