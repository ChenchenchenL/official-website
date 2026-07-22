package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDependencyEntity;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDependencyMapper;
import com.company.officialwebsite.modules.pagebuilder.service.PageDependencyQueryService;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDependencyVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PageDependencyQueryServiceImpl：页面发布依赖的分页查询实现。
 */
@Service
public class PageDependencyQueryServiceImpl implements PageDependencyQueryService {

    private final PageDefinitionMapper pageDefinitionMapper;
    private final PageDependencyMapper pageDependencyMapper;

    public PageDependencyQueryServiceImpl(
            PageDefinitionMapper pageDefinitionMapper,
            PageDependencyMapper pageDependencyMapper) {
        this.pageDefinitionMapper = pageDefinitionMapper;
        this.pageDependencyMapper = pageDependencyMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PageDependencyVO> getPublishedDependencies(Long pageId, int pageNo, int pageSize) {
        PageDefinitionEntity page = pageDefinitionMapper.selectById(pageId);
        if (page == null) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND);
        }

        int validPageNo = Math.max(1, pageNo);
        int validPageSize = Math.min(100, Math.max(1, pageSize));
        Page<PageDependencyEntity> queryPage = new Page<>(validPageNo, validPageSize);
        pageDependencyMapper.selectPage(queryPage,
                new LambdaQueryWrapper<PageDependencyEntity>()
                        .eq(PageDependencyEntity::getPageId, pageId)
                        .orderByAsc(PageDependencyEntity::getSnapshotId)
                        .orderByAsc(PageDependencyEntity::getComponentInstanceId)
                        .orderByAsc(PageDependencyEntity::getId));

        List<PageDependencyVO> records = queryPage.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(records, queryPage.getTotal(), validPageNo, validPageSize);
    }

    private PageDependencyVO toVO(PageDependencyEntity entity) {
        PageDependencyVO vo = new PageDependencyVO();
        vo.setSnapshotId(entity.getSnapshotId());
        vo.setComponentInstanceId(entity.getComponentInstanceId());
        vo.setDependencyType(entity.getDependencyType());
        vo.setTargetModule(entity.getTargetModule());
        vo.setTargetEntityType(entity.getTargetEntityType());
        vo.setTargetEntityId(entity.getTargetEntityId());
        return vo;
    }
}
