package com.company.officialwebsite.modules.content.service;

import com.company.officialwebsite.modules.content.constants.ContentReferenceConstants;
import com.company.officialwebsite.modules.content.constants.ContentReferenceKeyBuilder;
import com.company.officialwebsite.modules.content.entity.ContentReferenceEntity;
import com.company.officialwebsite.modules.product.entity.ProductEntity;
import com.company.officialwebsite.modules.product.mapper.ProductMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ContentReferenceQueryServiceTest {

    @Autowired
    private ContentReferenceService contentReferenceService;

    @Autowired
    private ProductMapper productMapper;

    @Test
    @DisplayName("内容引用同步与依赖查询测试")
    void syncAndQueryReferences_shouldWorkCorrectly() {
        // 先创建被引用的产品记录
        ProductEntity product = new ProductEntity();
        product.setName("测试产品引用");
        product.setLogoId(1L);
        product.setSubTitle("测试副标题");
        product.setAbstractText("测试摘要");
        product.setStatus("ACTIVE");
        product.setVisible(1);
        productMapper.insert(product);

        // 使用 ContentReferenceKeyBuilder 构造规范 Key，不使用自由字符串
        String snapshotKey = ContentReferenceKeyBuilder.forSolutionSnapshot(1L);

        ContentReferenceEntity ref1 = new ContentReferenceEntity();
        ref1.setReferencedType(ContentReferenceConstants.REFERENCED_TYPE_PRODUCT);
        ref1.setReferencedId(product.getId());
        ref1.setReferenceType(ContentReferenceConstants.REF_TYPE_RELATED_ENTITY);

        contentReferenceService.syncReferences(
                ContentReferenceConstants.REFERRER_TYPE_SOLUTION_SNAPSHOT,
                snapshotKey,
                List.of(ref1)
        );

        boolean hasRef = contentReferenceService.hasActiveReferences(
                ContentReferenceConstants.REFERENCED_TYPE_PRODUCT,
                product.getId()
        );
        assertThat(hasRef).isTrue();

        List<ContentReferenceEntity> refs = contentReferenceService.findReferencesByReferrer(
                ContentReferenceConstants.REFERRER_TYPE_SOLUTION_SNAPSHOT,
                snapshotKey
        );
        assertThat(refs).hasSize(1);
        assertThat(refs.get(0).getReferencedId()).isEqualTo(product.getId());
        // 验证 Key 格式符合规范
        assertThat(ContentReferenceKeyBuilder.parseId(snapshotKey)).isEqualTo(1L);
        assertThat(ContentReferenceKeyBuilder.parsePrefix(snapshotKey)).isEqualTo("solution_snapshot");
    }
}
