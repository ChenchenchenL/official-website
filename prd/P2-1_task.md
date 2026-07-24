# P2-1 草稿自动保存与修订历史任务拆解与自检清单 (task.md)

本文档将 **P2-1 草稿自动保存与修订历史整改** 拆化为具体的开发与测试任务清单，并提供自检门禁。

---

## 一、任务细分拆解清单 (Step-by-Step Task Breakdown)

### 阶段一：持久层与实体模型构建

- [x] **Task 1.1**: 创建 `PageDraftHistoryEntity.java` (位于 `modules/pagebuilder/entity/PageDraftHistoryEntity.java`)：
  * 对应表 `cms_page_draft_history`；字段包含 `id`, `pageId`, `draftId`, `revisionNo`, `schemaJson`, `schemaHash`, `editorSessionRemark`, `createdBy`, `createdAt`。
- [x] **Task 1.2**: 创建 `PageDraftHistoryMapper.java` (位于 `modules/pagebuilder/mapper/PageDraftHistoryMapper.java`)。
- [x] **Task 1.3**: 创建 `PageDraftHistoryVO.java` (位于 `modules/pagebuilder/vo/PageDraftHistoryVO.java`)。

---

### 阶段二：核心服务与历史自动裁剪逻辑

- [x] **Task 2.1**: 创建 `PageDraftHistoryService.java` 与 `PageDraftHistoryServiceImpl.java` (位于 `modules/pagebuilder/service/`)：
  * 实现 `recordRevision`: 自动计算递增 `revisionNo`、插入历史、自动裁剪保持最多 20 条。
  * 实现 `getRevisionsPage`: 支持草稿历史摘要分页列表查询。
  * 实现 `getRevisionDetail`: 查询单条历史完整 Schema 详情。
  * 实现 `restoreRevision`: 校验编辑锁与并发乐观锁，使用指定历史 Schema 覆盖草稿，自增乐观锁版本并记录审计日志。
- [x] **Task 2.2**: 在 `PageDraftServiceImpl.java` 中挂载历史记录逻辑：
  * 在 `saveDraft` 与 `resetDraftToPublished` 事务完成后自动触发 `recordRevision`。

---

### 阶段三：Admin API 控制器暴露与接口文档更新

- [x] **Task 3.1**: 更新 `AdminPageDraftController.java`：
  * 暴露 `GET /admin/api/page-builder/drafts/{pageId}/revisions`
  * 暴露 `GET /admin/api/page-builder/drafts/{pageId}/revisions/{revisionId}`
  * 暴露 `POST /admin/api/page-builder/drafts/{pageId}/revisions/{revisionId}/restore`
- [x] **Task 3.2**: 同步更新 `docs/接口文档.md`：
  * 补充上述 3 个 Admin 接口路径、参数、响应结构及 10003/10009/10011 错误码说明。

---

### 阶段四：自动化测试全覆盖

- [x] **Task 4.1**: 创建 `PageDraftHistoryServiceTest.java`：
  * 验证草稿历史记录生成与上限 20 条自动裁剪删除。
  * 验证分页列表与单条详情查询。
  * 验证恢复修订时的乐观锁冲突与编辑锁门禁。

---

## 二、关键难点与解决办法速查表

| 难点序号 | 场景说明 | 核心风险 | 解决办法 |
| :---: | :--- | :--- | :--- |
| **N1** | 恢复草稿历史时的并发覆盖 | 恢复历史时其他管理员同时保存造成覆盖 | 强门禁校验 `X-Editor-Lock-Token` 与乐观锁 `version`，冲突抛 `409` |
| **N2** | 草稿历史数据无限膨胀 | 频繁保存导致数据库空间暴涨 | 每次落盘后自动擦除该页面超过 20 条的最老历史 |

---

## 三、文档自检核验 (Self-Inspection Verification)

在进入代码实现前，对照以下项进行阅读自检：

- [x] **检查项 1**：草稿自动保存节流策略与并发 409 冲突响应机制是否确定？（已明确带回最新 VO，不静默覆盖）
- [x] **检查项 2**：`cms_page_draft_history` 表结构与实体/VO模型设计是否规范？（已建立完整的持久化与 VO 结构）
- [x] **检查项 3**：草稿历史保留上限 (20 条) 与自动擦除逻辑是否清晰？（已设计落盘后自动物理清理最老超额数据）
- [x] **检查项 4**：恢复草稿修订 Admin API 的乐观锁与编辑锁门禁是否严密？（已强制要求 version 与 Lock-Token 校验）
- [x] **检查项 5**：接口文档与单元测试计划是否健全？（涵盖 API 文档补全与完整的逻辑单测）

---

**自检结论**：自检完全通过，文档完整严密。
