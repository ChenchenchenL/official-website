package com.company.officialwebsite.modules.product.mapper;

import com.company.officialwebsite.modules.product.entity.ProductDraftEntity;
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
class ProductDraftMapperTest {

    @Autowired
    private ProductDraftMapper productDraftMapper;

    @Test
    @DisplayName("产品草稿插入与逻辑删除联合唯一索引测试")
    void productDraft_crudAndUniqueIndexTest() {
        ProductDraftEntity draft1 = new ProductDraftEntity();
        draft1.setProductId(101L);
        draft1.setDraftJson("{\"title\":\"产品草稿测试\"}");
        draft1.setDraftHash("hash_101");
        draft1.setEditorSessionRemark("新增试用");
        draft1.setCreatedBy("admin");

        int rows = productDraftMapper.insert(draft1);
        assertThat(rows).isEqualTo(1);
        assertThat(draft1.getId()).isNotNull();

        ProductDraftEntity duplicate = new ProductDraftEntity();
        duplicate.setProductId(101L);
        duplicate.setDraftJson("{\"title\":\"重复草稿\"}");
        duplicate.setDraftHash("hash_101_dup");

        assertThatThrownBy(() -> productDraftMapper.insert(duplicate))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
