package com.company.officialwebsite.modules.content.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.company.officialwebsite.common.dto.DetailDraftSaveDTO;
import com.company.officialwebsite.common.dto.DetailOfflineDTO;
import com.company.officialwebsite.common.dto.DetailPublishDTO;
import com.company.officialwebsite.common.dto.DetailRollbackDTO;
import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AbstractDetailEditorServiceTest：验证详情编辑模板在所有受保护写操作前统一校验独占编辑锁。
 */
@ExtendWith(MockitoExtension.class)
class AbstractDetailEditorServiceTest {

    @Mock
    private EditorLockService editorLockService;

    @Test
    void protectedWriteOperations_shouldValidateLockBeforeDelegating() {
        TestDetailEditorService service = new TestDetailEditorService(editorLockService);

        service.saveDraft(100L, new DetailDraftSaveDTO(), "lock-token", "admin");
        service.publish(100L, new DetailPublishDTO(), "lock-token", "admin");
        service.rollback(100L, 200L, new DetailRollbackDTO(), "lock-token", "admin");
        service.offline(100L, new DetailOfflineDTO(), "lock-token", "admin");

        verify(editorLockService, times(4))
                .validateLock(EditorResourceTypeEnum.PRODUCT, 100L, "lock-token", "admin");
    }

    private static final class TestDetailEditorService extends AbstractDetailEditorService<String, String> {

        private TestDetailEditorService(EditorLockService editorLockService) {
            super(editorLockService);
        }

        @Override
        public EditorResourceTypeEnum getResourceType() {
            return EditorResourceTypeEnum.PRODUCT;
        }

        @Override
        public String getDraft(Long resourceId) {
            return "draft";
        }

        @Override
        protected String doSaveDraft(Long resourceId, DetailDraftSaveDTO saveDTO, String operator) {
            return "draft";
        }

        @Override
        protected String doPublish(Long resourceId, DetailPublishDTO publishDTO, String operator) {
            return "version";
        }

        @Override
        protected String doRollback(
                Long resourceId,
                Long targetVersionId,
                DetailRollbackDTO rollbackDTO,
                String operator) {
            return "version";
        }

        @Override
        protected void doOffline(Long resourceId, DetailOfflineDTO offlineDTO, String operator) {
        }
    }
}
