package com.company.officialwebsite.modules.casecenter.mapper;

import com.company.officialwebsite.modules.casecenter.entity.CaseDraftEntity;
import com.company.officialwebsite.modules.casecenter.entity.CaseVersionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CaseDraftAndVersionMapperTest {

    @Autowired
    private CaseDraftMapper caseDraftMapper;

    @Autowired
    private CaseVersionMapper caseVersionMapper;

    @Test
    @DisplayName("案例草稿与版本快照持久化测试")
    void caseDraftAndVersion_crudTest() {
        CaseDraftEntity draft = new CaseDraftEntity();
        draft.setCaseId(202L);
        draft.setDraftJson("{\"title\":\"案例草稿\"}");
        draft.setDraftHash("hash_c202");
        caseDraftMapper.insert(draft);

        assertThat(draft.getId()).isNotNull();

        CaseVersionEntity version = new CaseVersionEntity();
        version.setCaseId(202L);
        version.setVersionNo(1);
        version.setSnapshotJson("{\"title\":\"案例快照\"}");
        version.setSnapshotHash("hash_c202");
        version.setPublisher("admin");
        version.setPublishedAt(LocalDateTime.now());
        caseVersionMapper.insert(version);

        assertThat(version.getId()).isNotNull();
        assertThat(caseVersionMapper.selectById(version.getId()).getVersionNo()).isEqualTo(1);
    }
}
