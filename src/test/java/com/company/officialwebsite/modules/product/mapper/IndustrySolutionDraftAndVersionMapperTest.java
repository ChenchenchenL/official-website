package com.company.officialwebsite.modules.product.mapper;

import com.company.officialwebsite.modules.product.entity.IndustrySolutionDraftEntity;
import com.company.officialwebsite.modules.product.entity.IndustrySolutionVersionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class IndustrySolutionDraftAndVersionMapperTest {

    @Autowired
    private IndustrySolutionDraftMapper industrySolutionDraftMapper;

    @Autowired
    private IndustrySolutionVersionMapper industrySolutionVersionMapper;

    @Test
    @DisplayName("行业方案草稿与版本快照持久化测试")
    void solutionDraftAndVersion_crudTest() {
        IndustrySolutionDraftEntity draft = new IndustrySolutionDraftEntity();
        draft.setSolutionId(303L);
        draft.setDraftJson("{\"title\":\"方案草稿\"}");
        draft.setDraftHash("hash_s303");
        industrySolutionDraftMapper.insert(draft);

        assertThat(draft.getId()).isNotNull();

        IndustrySolutionVersionEntity version = new IndustrySolutionVersionEntity();
        version.setSolutionId(303L);
        version.setVersionNo(1);
        version.setSnapshotJson("{\"title\":\"方案快照\"}");
        version.setSnapshotHash("hash_s303");
        version.setPublisher("admin");
        version.setPublishedAt(LocalDateTime.now());
        industrySolutionVersionMapper.insert(version);

        assertThat(version.getId()).isNotNull();
        assertThat(industrySolutionVersionMapper.selectById(version.getId()).getVersionNo()).isEqualTo(1);
    }
}
