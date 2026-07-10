# 武汉云台官网 Release Baseline v1.0

生成日期：2026-07-08

## 代码基线

| 仓库 | 分支 | HEAD |
| --- | --- | --- |
| `official-website` | `main` | `0d23915` |
| `yuntai-frontend` | `main` | `d6c0703` |

说明：本文件记录生成时的 HEAD，当前工作区仍包含未提交变更，应以最终提交后的 commit 作为正式发布标签。

## 数据库基线

Flyway 当前基线版本：`V1` 至 `V33`。

新增发布前迁移：

- `V23__extend_media_asset_for_library.sql`
- `V24__create_page_section.sql`
- `V25__add_content_status_to_product_and_case.sql`
- `V26__create_content_tag_table.sql`
- `V27__create_content_category_table.sql`
- `V28__create_content_relation_table.sql`
- `V29__create_content_reference_table.sql`
- `V30__create_business_registry_table.sql`
- `V31__create_business_template_table.sql`
- `V32__create_business_block_table.sql`
- `V33__create_business_page_tables.sql`

历史迁移治理口径：`V1` 至 `V22` 不作为本次发布修改对象；如需兼容索引语法，应新增后续迁移承接。

## 接口基线

Controller 注解统计口径：

| 类型 | 数量 |
| --- | ---: |
| Admin 接口 | 167 |
| Portal 接口 | 23 |
| 合计 | 190 |

P0 契约已纳入：

- 页面区块：`/admin/api/site/page-sections`、`/portal/api/site/page-sections`
- 媒体库：`/admin/api/media/assets`
- 产品详情：`/portal/api/products/{id}`
- 案例详情：`/portal/api/cases/{id}`

P1/P2 契约已纳入：

- Dashboard：`/admin/api/dashboard/*`
- 内容治理：`content-tags/categories/relations/references`
- 业务编排：`business-registry/templates/blocks/pages/page-blocks`

## 已知问题

- 内容治理模块已具备后端接口和通用 Admin 入口，但精细化运营表单仍可后续迭代。
- 业务编排模块已具备基础接口，暂不作为官网前台直接消费能力。
- 前台仍保留 fallback 数据，用于接口失败、空数组和 `contentJson` 解析失败时保障展示。
- 当前工作区存在发布前收口变更，需完成验证后再创建正式 release tag。

## 验证要求

后端：

```bash
git diff --check
mvn -q test
```

前端：

```bash
git diff --check
npm run build
```

Maven 或 Node 环境不可用时，应在交付说明中明确记录未执行原因。
