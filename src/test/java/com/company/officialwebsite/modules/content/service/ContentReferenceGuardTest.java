package com.company.officialwebsite.modules.content.service;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDependencyEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PagePublishSnapshotEntity;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDependencyMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PagePublishSnapshotMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ContentReferenceGuardTest {

    @Autowired
    private ContentReferenceGuard contentReferenceGuard;

    @Autowired
    private PageDependencyMapper pageDependencyMapper;

    @Autowired
    private PagePublishSnapshotMapper pagePublishSnapshotMapper;

    @Test
    @DisplayName("已发布页面存在强引用时阻断下线抛出 409 Conflict")
    void assertNotReferencedByPage_shouldThrow409_whenActiveSnapshotReferencesContent() {
        PagePublishSnapshotEntity snapshot = insertSnapshot(101L, "ACTIVE");
        insertDependency(101L, snapshot.getId(), "888");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                contentReferenceGuard.assertNotReferencedByPage("product", "Product", 888L));

        assertEquals(ErrorCode.RESOURCE_REFERENCE_CONFLICT, ex.getErrorCode());
    }

    @Test
    @DisplayName("SUPERSEDED 历史快照引用不阻断内容下线")
    void assertNotReferencedByPage_shouldNotThrow_whenOnlySupersededSnapshotReferencesContent() {
        PagePublishSnapshotEntity snapshot = insertSnapshot(102L, "SUPERSEDED");
        insertDependency(102L, snapshot.getId(), "888");

        contentReferenceGuard.assertNotReferencedByPage("product", "Product", 888L);
    }

    @Test
    @DisplayName("无对应发布快照的依赖记录不阻断内容下线")
    void assertNotReferencedByPage_shouldNotThrow_whenDependencyHasNoSnapshot() {
        insertDependency(103L, 9999L, "888");

        contentReferenceGuard.assertNotReferencedByPage("product", "Product", 888L);
    }

    private PagePublishSnapshotEntity insertSnapshot(Long pageId, String status) {
        PagePublishSnapshotEntity snapshot = new PagePublishSnapshotEntity();
        snapshot.setPageId(pageId);
        snapshot.setVersionId(pageId);
        snapshot.setSnapshotHash("snapshot-hash-" + pageId);
        snapshot.setPublishStatus(status);
        pagePublishSnapshotMapper.insert(snapshot);
        return snapshot;
    }

    private void insertDependency(Long pageId, Long snapshotId, String targetId) {
        PageDependencyEntity dep = new PageDependencyEntity();
        dep.setPageId(pageId);
        dep.setSnapshotId(snapshotId);
        dep.setComponentInstanceId("sec_product_" + pageId);
        dep.setDependencyType("ENTITY");
        dep.setTargetModule("product");
        dep.setTargetEntityType("Product");
        dep.setTargetEntityId(targetId);
        pageDependencyMapper.insert(dep);
    }
}
