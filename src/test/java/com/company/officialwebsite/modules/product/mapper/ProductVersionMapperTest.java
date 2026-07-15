package com.company.officialwebsite.modules.product.mapper;

import com.company.officialwebsite.modules.product.entity.ProductVersionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ProductVersionMapperTest {

    @Autowired
    private ProductVersionMapper productVersionMapper;

    @Test
    @DisplayName("产品发布版本快照插入及 rollbackSourceVersionId 持久化测试")
    void productVersion_crudAndRollbackSourceTest() {
        ProductVersionEntity v1 = new ProductVersionEntity();
        v1.setProductId(101L);
        v1.setVersionNo(1);
        v1.setSnapshotJson("{\"title\":\"版本1\"}");
        v1.setSnapshotHash("hash_v1");
        v1.setChangeSummary("初始发布");
        v1.setPublisher("admin");
        v1.setPublishedAt(LocalDateTime.now());

        productVersionMapper.insert(v1);

        // 回滚生成版本2，标记 rollbackSourceVersionId
        ProductVersionEntity v2 = new ProductVersionEntity();
        v2.setProductId(101L);
        v2.setVersionNo(2);
        v2.setSnapshotJson("{\"title\":\"版本1\"}");
        v2.setSnapshotHash("hash_v1");
        v2.setChangeSummary("回滚至版本1");
        v2.setPublisher("admin");
        v2.setRollbackSourceVersionId(v1.getId());
        v2.setPublishedAt(LocalDateTime.now());

        productVersionMapper.insert(v2);

        ProductVersionEntity fetchedV2 = productVersionMapper.selectById(v2.getId());
        assertThat(fetchedV2).isNotNull();
        assertThat(fetchedV2.getRollbackSourceVersionId()).isEqualTo(v1.getId());

        // 重复插入相同 product_id + version_no 触发表级唯一约束
        ProductVersionEntity v2Dup = new ProductVersionEntity();
        v2Dup.setProductId(101L);
        v2Dup.setVersionNo(2);
        v2Dup.setSnapshotJson("{\"title\":\"重复版本2\"}");
        v2Dup.setSnapshotHash("hash_v2_dup");
        v2Dup.setPublisher("admin");
        v2Dup.setPublishedAt(LocalDateTime.now());

        assertThatThrownBy(() -> productVersionMapper.insert(v2Dup))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
