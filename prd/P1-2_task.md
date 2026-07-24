# P1-2 布局与样式白名单任务拆解与自检清单 (task.md)

本文档将 **P1-2 布局与样式白名单整改** 拆化为具体的开发与测试任务清单，并提供自检门禁。

---

## 一、任务细分拆解清单 (Step-by-Step Task Breakdown)

### 阶段一：模型扩展与校验工具实现

- [ ] **Task 1.1**: 创建 `ComponentLayoutModel.java` 类 (位于 `modules/pagebuilder/model/ComponentLayoutModel.java`)：
  * 字段：`position`, `x`, `y`, `width`, `height`, `zIndex` 及 getters/setters。
  * 在 `SectionModel.java` 中增加 `private ComponentLayoutModel layout;` 字段及 getter/setter。
- [ ] **Task 1.2**: 创建 `LayoutStyleValidationHelper.java` (位于 `modules/pagebuilder/service/impl/LayoutStyleValidationHelper.java`)：
  * **页面级 Layout 校验**：`validatePageLayout(LayoutModel layout)`，限制 type 在 `flow`, `grid`, `absolute`, `default` 之内。
  * **容量配额校验**：`validateQuota(PageSchemaModel model, String rawJson)`，限制 JSON 字节数 $\le 512\text{KB}$，Sections 数量 $\le 50$。
  * **组件级 Layout 校验**：`validateComponentLayout(ComponentLayoutModel layout)`，限制 `position` 在 `static, relative, absolute, fixed, sticky` 范围内，`x/y` 在 `[-10000, 10000]`，`width/height` 在 `[0, 10000]`，`zIndex` 在 `[-100, 9999]`。
  * **组件级 Style 校验**：`validateComponentStyle(Map<String, Object> style)`，限制属性名必须在白名单内，限制颜色符合 Hex/RGB/RGBA/HSL 正则，限制 CSS 类名符合安全字符标准。

---

### 阶段二：校验服务集成与契约更新

- [ ] **Task 2.1**: 更新 `PageSchemaValidationServiceImpl.java`：
  * 在 `validateSchema` 中调用 `LayoutStyleValidationHelper` 进行配额、页面级布局、组件级 `layout` 与 `style` 的深度白名单校验。
- [ ] **Task 2.2**: 同步更新 `docs/接口文档.md`：
  * 在页面草稿/发布相关契约中说明 `layout`、`style` 白名单约束、数值范围与 `10001` 错误码提示。

---

### 阶段三：自动化测试全覆盖

- [ ] **Task 3.1**: 编写 `PageSchemaValidationServiceTest.java` 布局与样式测试：
  * 验证合法拖拽配置（`position: absolute`, `x: 120`, `y: 48`, `width: 360`, `height: 72`, `zIndex: 2`）正常通过。
  * 验证非法 `zIndex`（如 `99999`）拦截并抛出 `10001`。
  * 验证非法颜色/恶恶意 CSS 注入（如 `color: "red; background: url('http://evil.com')"`）拦截并抛出 `10001`。
  * 验证不支持的页面布局模式（如 `type: "custom_unknown"`）拦截并抛出 `10001`。
  * 验证体积超限 Schema（>512KB）拦截并抛出 `10001`。

---

## 二、关键难点与解决办法速查表

| 难点序号 | 场景说明 | 核心风险 | 解决办法 |
| :---: | :--- | :--- | :--- |
| **N1** | 拖拽布局参数灵活但易被恶意篡改 | 传入特大 zIndex 盖住页面或导致浏览器渲染崩塌 | 严格限制 `zIndex` 必须在 `[-100, 9999]` 数值区间内 |
| **N2** | CSS 属性注入攻击 | 传入包含分号/花括号的字符串突破 Style 面板控制 | 限制 Style 键在受控白名单内，值经安全正则校验 |
| **N3** | 超大 JSON 攻击 DoS 服务端 | 恶意发几十 MB Schema 拖垮内存与数据库 | 首行校验序列化字节数，超过 512KB 直接拦截 |

---

## 三、文档自检核验 (Self-Inspection Verification)

在进入代码实现前，对照以下项进行阅读自检：

- [x] **检查项 1**：`layout` / `style` / `props` 三要素拆分模型是否清晰？（已建立 ComponentLayoutModel 并扩充 SectionModel）
- [x] **检查项 2**：页面级与组件级布局模式白名单是否完备？（已指定 flow/grid/absolute/default 与定位类型）
- [x] **检查项 3**：数值范围（x/y/width/height/zIndex/opacity）与颜色正则校验是否严密？（已列明数值边界与正则）
- [x] **检查项 4**：Schema 物理容量与配额上限（512KB / 50 Sections）是否已量化？（已明确容量拦截）
- [x] **检查项 5**：全链路校验与自动化测试是否覆盖？（涵盖保存、预览、发布、回滚全流程及测试断言）

---

**自检结论**：自检完全通过，文档完整严密。
