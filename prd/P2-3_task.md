# P2-3 复用区块与模板建页任务拆解与自检清单 (task.md)

本文档将 **P2-3 复用区块与模板建页整改** 拆化为具体的开发与测试任务清单，并提供自检门禁。

---

## 一、任务细分拆解清单 (Step-by-Step Task Breakdown)

### 阶段一：实体扩充与复制请求 DTO 构建

- [x] **Task 1.1**: 创建 `PageCopyDTO.java` (位于 `modules/pagebuilder/dto/PageCopyDTO.java`)：
  * 字段包含 `sourcePageId`, `sourceTemplateCode`, `targetName`, `targetPath`, `targetPageKey` 等。
- [x] **Task 1.2**: 更新 `PageDefinitionEntity.java` 与 `PageDefinitionVO.java`：
  * 追加 `sourcePageId` 与 `sourceTemplateCode` 字段及 Getter/Setter。

---

### 阶段二：页面复制与组件 ID 重新分配服务实现

- [x] **Task 2.1**: 创建 `PageCopyService.java` 与 `PageCopyServiceImpl.java` (位于 `modules/pagebuilder/service/`)：
  * 实现 `copyPage`: 强校验目标 `targetPath` 与 `targetPageKey` 唯一性防冲突，为派生组件重新分配 Section ID，创建页面与初始草稿。
  * 实现 `diagnoseSharedBlockImpact`: 扫描并诊断共享区块影响的页面范围。

---

### 阶段三：Admin API 控制器暴露与接口文档更新

- [x] **Task 3.1**: 更新/创建 Admin 控制器：
  * 暴露 `POST /admin/api/page-builder/pages/copy`
  * 暴露 `GET /admin/api/page-builder/shared-blocks/{blockId}/impact`
- [x] **Task 3.2**: 同步更新 `docs/接口文档.md`：
  * 补充复制建页与共享区块影响诊断 API 契约说明。

---

### 阶段四：自动化测试全覆盖

- [x] **Task 4.1**: 创建 `PageCopyServiceTest.java`：
  * 验证成功复制页面且重新分配组件 Section ID。
  * 验证路由 Path 或 PageKey 冲突时拦截抛出 `10001`。
  * 验证源页面不存在时拦截抛出 404 错误。

---

## 二、关键难点与解决办法速查表

| 难点序号 | 场景说明 | 核心风险 | 解决办法 |
| :---: | :--- | :--- | :--- |
| **N1** | 组件 Section ID 重复污染 | 直接复制遗留旧组件 ID 导致多页间样式与状态绑定混淆 | 复制过程中重新为每个 Section 生成全新 UUID ID |
| **N2** | URL 路由与 PageKey 冲突 | 继承源页面的 Path/Key 触发数据库唯一索引冲突 | 强制要求传入新 Path/Key 并强校验唯一性 |

---

## 三、文档自检核验 (Self-Inspection Verification)

在进入代码实现前，对照以下项进行阅读自检：

- [x] **检查项 1**：页面复制与基于模板建页方案选型是否明确？（已确立 `PageCopyDTO` 机制与路径强校验）
- [x] **检查项 2**：路由 Path 与 PageKey 唯一性防冲突规则是否确定？（已确立 `deleted_marker = 0` 范围内的强制唯一性校验）
- [x] **检查项 3**：组件 Section ID 重新分配算法是否清晰？（已确立复制时自动替换新 UUID ID）
- [x] **检查项 4**：共享区块影响诊断机制是否明确？（已建立全局受影响页面扫描诊断接口）
- [x] **检查项 5**：接口文档更新与单元测试计划是否健全？（涵盖 API 文档补全与完整的复制/防冲突单测）

---

**自检结论**：自检完全通过，文档完整严密。
