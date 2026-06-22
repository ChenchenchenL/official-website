# Backend Development Rules

Applicable stack: Java, Spring Boot, MyBatis Plus, MySQL. This document constrains backend implementation for the Admin system and Portal APIs of the official website. The priorities are maintainability, data consistency, security boundaries, and testability.

---

## 1. General Principles

* **Business-first layering**: Organize code by business module. Do not flatten all entities, Mappers, Services, and Controllers into global packages.
* **Separate interface from implementation**: Controllers handle HTTP input/output adaptation. Application Services handle cross-module orchestration and read-only aggregation. Services handle business rules, transaction boundaries, and domain validation. Mappers handle database access only.
* **Explicit over implicit**: Key fields, visible/hidden rules, cache invalidation, permission checks, and transaction propagation must be encoded explicitly and must not rely on callers "remembering the convention".
* **Rollback-oriented design**: Critical behaviors such as show, hide, delete, export, and view-sensitive-data must have audit logs and necessary data snapshots.
* **Stable Portal output**: Portal response structures must remain stable and use consistent field naming. Backend table structure changes must not directly leak into Portal output.

---

## 2. Project Structure Rules

The backend source tree must be split by business module as follows:

```text
src/main/java/com/company/officialwebsite
├── OfficialWebsiteApplication.java
├── common
│   ├── config
│   ├── constants
│   ├── enums
│   ├── exception
│   ├── response
│   ├── trace
│   └── utils
├── application
│   ├── admin
│   └── portal
├── modules
│   ├── site
│   │   ├── controller
│   │   ├── service
│   │   ├── mapper
│   │   ├── entity
│   │   ├── dto
│   │   ├── vo
│   │   └── converter
│   ├── product
│   ├── casecenter
│   ├── news
│   ├── lead
│   ├── media
│   └── system
└── infrastructure
    ├── cache
    ├── storage
    ├── security
    └── audit
```

Resource and test directory conventions:

```text
src/main/resources
├── mapper
└── db
    └── migration

src/test/java/com/company/officialwebsite
├── common
├── application
│   ├── admin
│   └── portal
├── modules
│   ├── site
│   ├── product
│   ├── casecenter
│   ├── news
│   ├── lead
│   ├── media
│   └── system
└── support
    ├── fixture
    ├── factory
    ├── fake
    └── assertion
```

* **`common`**: Common capabilities only. No concrete business logic.
* **`application`**: Cross-module orchestration, Portal aggregation queries, and read-only VO assembly. Use this layer to avoid cyclic Service dependencies between business modules.
* **`modules`**: Each business module independently owns its Controller, Service, Mapper, Entity, DTO, and VO.
* **`infrastructure`**: Cache, object storage, security, audit, and similar infrastructure adapters.
* **No direct cross-module Mapper access**: If one module needs another module's data, expose it through that module's Service, query service, or application-layer orchestration.
* **No cyclic dependencies**: Modules must not form mutual call loops. Cross-module aggregation should prefer the application layer or an event mechanism.

---

## 3. Naming Rules

* **Entity**: Must correspond to database tables. Use `XxxEntity` or `Xxx`, but keep the convention consistent within the project.
* **DTO**: Used for request payloads or internal commands, such as `ProductCreateDTO`, `ProductUpdateDTO`, `ProductQueryDTO`.
* **VO**: Used for API responses, such as `ProductDetailVO`, `PortalHomeVO`.
* **Mapper**: MyBatis Plus mapper, named `XxxMapper`.
* **Service**: Interface named `XxxService`, implementation named `XxxServiceImpl`.
* **Controller**: Admin API controllers use `AdminXxxController`. Portal API controllers use `PortalXxxController`.
* **Converter**: Object conversion class named `XxxConverter`, responsible for Entity, DTO, and VO conversion.
* **Enum**: Business statuses must be expressed as enums, such as `ContentVisibilityEnum`, `MenuTargetTypeEnum`.

---

## 4. Controller Rules

* **Path separation**: Admin APIs must use `/admin/api/**`. Portal APIs must use `/portal/api/**`.
* **Responsibility limit**: Controllers only receive parameters, perform basic annotation-based validation, call Services or Application Services, and return unified responses. They must not contain database queries or stateful business rules. Lightweight stateless parameter adaptation is allowed, but cross-module aggregation belongs in the application layer.
* **Unified response**: All APIs must return a unified structure. Do not return Entity objects, `Map`, or raw strings directly.
* **Parameter validation**: Request DTOs must use Bean Validation annotations such as `@NotBlank`, `@NotNull`, and `@Size`.
* **Pagination parameters**: Paginated APIs must use `pageNo` and `pageSize`, with a maximum allowed `pageSize`.
* **No Entity passthrough**: Controllers must not directly receive or return database Entities, to avoid field leakage and schema coupling.

Recommended unified response structure:

```java
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
    private String traceId;
}
```

---

## 5. Service Rules

* **Transaction boundary belongs in Service**: Operations involving multi-table writes, show/hide transitions, or audit logs must define transactions at the Service layer. Cache invalidation must be registered as a post-commit action.
* **Read-only transactions**: Query methods may use `@Transactional(readOnly = true)`. Write methods must declare transaction semantics explicitly.
* **Visibility validation**: Show, hide, delete, and similar operations must validate whether the current data state allows the transition. Direct state overwrite is not allowed.
* **Duplicate data validation**: Unique rules such as menu path, page path, product name, and category name must be validated in the Service layer, with database unique indexes as the final safety net.
* **Cache invalidation**: After successful Admin save, show/hide, or delete operations, the corresponding Portal cache invalidation must be triggered only after transaction commit through `TransactionSynchronizationManager.afterCommit`, `@TransactionalEventListener(phase = AFTER_COMMIT)`, or an equivalent mechanism. Do not delete cache directly at the end of the transactional method.
* **No swallowed exceptions**: Do not catch exceptions and only print logs without rethrowing. Recoverable exceptions must be handled explicitly. Non-recoverable exceptions must go to the global exception handler.
* **Audit logs**: Critical write operations must record operator, module, target object, before/after core content, operation time, and request source.

---

## 6. MyBatis Plus and Data Access Rules

* **Keep Mappers simple**: Use MyBatis Plus for simple CRUD. Complex queries may use XML, but the SQL must remain readable and maintainable.
* **Use wrappers carefully**: Prefer `LambdaQueryWrapper` when query conditions are numerous, to avoid string field names and refactor risk.
* **No full-table update/delete**: Every update/delete must include explicit conditions. Batch operations must limit the size of ID collections.
* **Soft delete**: Content-oriented data should use logical delete rather than physical delete. To avoid conflicts between soft delete and unique indexes, use `deleted_marker BIGINT NOT NULL DEFAULT 0` consistently: active rows use `0`, deleted rows store the row ID or another globally unique deletion marker.
* **Optimistic locking**: Core tables with frequent edits or overwrite risk should use a `version` field.
* **Field auto-fill**: `created_at`, `updated_at`, `created_by`, and `updated_by` should be auto-filled through a unified `MetaObjectHandler`.
* **XML SQL**: Complex SQL must avoid `select *`; fields must be listed explicitly. Dynamic SQL must guard against empty conditions causing full scans.
* **Pagination**: Use a pagination plugin or explicit `limit`. Admin list APIs must not return large datasets without pagination.

---

## 7. Database Design Rules

* **Table naming**: Use lowercase snake_case, with module prefixes such as `cms_product`, `cms_news_article`, `sys_user`.
* **Primary key**: Use `BIGINT` consistently. Snowflake ID, database auto-increment, or another team-standard strategy may be used, but strategies must not be mixed.
* **Time fields**: Use `created_at` and `updated_at`, preferably with `DATETIME`.
* **Operator fields**: Critical business tables should keep `created_by` and `updated_by`.
* **Status fields**: At the current stage, content visibility is expressed using visible/hidden plus logical delete fields. If drafts, approval, scheduled publish, or other complex states are needed, the PRD and testing rules must be updated first.
* **Sorting field**: Tables that require frontend ordering must provide `sort_order`, where smaller numbers appear first.
* **Soft delete field**: Use `deleted_marker` consistently. All Portal queries must by default query only active rows where `deleted_marker = 0`, or explicitly filter out deleted rows where `deleted_marker != 0`.
* **Index requirements**: Query conditions, sorting fields, and unique business keys must have appropriate indexes, such as `visible`, `category_id`, `sort_order`, `slug`, `deleted_marker`.
* **Unique constraints**: Unique rules such as page path, menu path, category code, and role code must be enforced by database unique indexes. Soft-delete tables must use composite unique indexes in the form of `(business_key, deleted_marker)`. Do not rely on boolean delete flags such as `(business_key, is_deleted)` for uniqueness.
* **Large field separation**: Rich text bodies, version snapshots, and audit details should be split from high-frequency list fields or queried with care.

---

## 8. Transaction and Consistency Rules

* **A single business write must be atomic**: For example, when updating content, visible state updates, media reference maintenance, audit logging, and cache invalidation registration must remain consistent on the database side.
* **Cache invalidation timing**: Cache deletion must happen after the database transaction commits, to avoid rollback cases where cache is already invalidated or rebuilt with dirty data. In Spring, use `TransactionSynchronizationManager.registerSynchronization(... afterCommit ...)`, transactional event listeners, or a wrapped `AfterCommitExecutor`.
* **External system calls**: Calls to object storage, notification systems, statistics systems, and similar external components should not stay inside long database transactions. Use events or async jobs when needed.
* **Idempotency**: APIs for show, hide, delete, callbacks, and message consumption must consider repeated invocation and produce consistent results.
* **Concurrent editing**: Core content editing should prefer `version` or updated-time checks to avoid late writes overwriting earlier committed writes.

---

## 9. Exception Handling Rules

* **Business exceptions**: Use a unified `BusinessException` for expected business errors such as duplicate names, invalid state transitions, or missing resources.
* **System exceptions**: Unknown exceptions must be caught by the global exception handler, return a generic error response, and log the full stack trace.
* **Error code management**: Error codes must use stable numeric codes and be segmented by module. Concrete values must be centrally maintained in a unified `ErrorCode` enum. Do not scatter raw numbers or untraceable temporary codes.
* **Validation errors**: Validation failures must return field-level error information so the frontend can guide user correction.
* **No internal detail leakage**: API responses must not expose SQL, stack traces, server paths, class names, or other internal details.

---

## 10. Logging Rules

* **Request tracing**: Every request must generate or propagate a `traceId`, and logs must keep it consistent with API responses.
* **Critical logs**: Login, show/hide, delete, export, sensitive-data view, upload failure, and cache rebuild failure must be logged.
* **Log level**: Use `warn` for business failures and `error` for system exceptions. Do not overuse `info` for normal paths.
* **Sensitive data**: Logs must not print passwords, tokens, full phone numbers, full email addresses, ID numbers, or similar sensitive data.
* **Structured logs**: Critical operation logs should include user ID, module, object ID, action, result, duration, and `traceId`.

---

## 11. Cache Rules

* **Cache targets**: High-frequency Portal reads such as global configuration, navigation, homepage data, product categories, and recommended content may use cache.
* **Cache implementation**: Production multi-instance deployment must use Redis or an equivalent shared distributed cache. Local JVM cache is limited to development, testing, or explicitly single-instance deployment.
* **Cache key naming**: Cache keys must have a unified prefix and module name, such as `official:portal:home`.
* **Cache TTL**: All Portal cache entries must have expiration time. Permanent cache is forbidden. Hot configuration caches may use relatively short TTL as a dirty-data fallback.
* **Cache invalidation**: After Admin content save, show/hide, delete, or sort adjustment, the related cache must be deleted immediately after transaction commit and a delayed second delete must also be scheduled, reducing the risk of stale reads being written back into cache under concurrency.
* **Penetration protection**: For non-existent but frequently requested data, short-lived empty-result cache is allowed.
* **Breakdown protection**: Hotspot cache rebuild should consider locks, async refresh, or short TTL to avoid many concurrent requests hitting the database at once.
* **No sensitive data cache by default**: If sensitive data such as Admin user permissions or full lead contact info must be cached, it must have strict expiration and isolation strategy.

---

## 12. Configuration Rules

* **Environment isolation**: Development, testing, staging, and production configurations must be managed separately. Do not hardcode environment differences in code.
* **Sensitive configuration**: Database passwords, object storage keys, JWT secrets, notification webhooks, and similar secrets must not be committed to the repository.
* **Config naming**: Custom configuration should use a project prefix, such as `official.storage.*` and `official.cache.*`.
* **Use defaults carefully**: Production-critical settings must not rely on dangerous defaults such as allowing all CORS origins, disabling auth, or unlimited upload size.

---

## 13. File Upload Rules

* **Single upload entry**: All image, video, PDF, and other uploads must go through the media module. Business modules must not implement their own upload logic.
* **File validation**: Validate extension, MIME type, file size, and image dimensions. Uploading executable scripts is forbidden.
* **Path generation**: Storage paths must be generated by the backend. Never use user-provided original filenames as final storage paths.
* **Metadata recording**: Record filename, size, format, width/height, storage path, access URL, uploader, and upload time.
* **Image processing**: Compression, thumbnails, WebP conversion, and similar expensive processing must not block the upload request thread. Use async jobs, background queues, or delayed generation. The upload API should first return the original file or another already available usable version.
* **Media status**: Media assets must record statuses such as `TEMPORARY`, `BOUND`, `UNBOUND`, and `DELETED`. After business save succeeds, temporary assets become bound. After business content is updated, media is replaced, or content is logically deleted, if the asset is no longer referenced by any valid content, it should become unbound.
* **Media references**: Business content and media assets must maintain reference relationships. Direct field references and rich text references must both be persisted. Scheduled cleanup may reclaim only expired `TEMPORARY` and `UNBOUND` assets.
* **Rich text media references**: Rich text save must not depend on backend HTML parsing to determine binding. The editor or API request must submit a media ID list or reference list, and the backend must maintain binding through that list. HTML is only for whitelist sanitization and rendering.
* **Failure cleanup**: If database save fails, uploaded but not successfully bound temporary files should be cleaned up immediately, or reclaimed later by scheduled jobs based on temporary/unbound status and expiration time.

---

## 14. Code Style Rules

* **File responsibility description**: Every Java source file must have a short top-level responsibility description, typically class-level Javadoc, explaining what the file does or which layer it serves, for example `ApiResponse: unified response wrapper.` Focus on responsibility only. Do not include author, date, or serial-number style metadata.
* **Necessary comments**: Public classes, public methods, core business rules, exception branches, permission checks, cache invalidation, post-commit actions, and logic behind complex SQL must have necessary comments or Javadoc so collaborators can quickly understand intent.
* **Comments explain intent**: Comments should explain why the code is written this way and what business constraint it enforces. Do not repeat obvious code meaning such as "set name" or "return result".
* **Keep comments in sync**: When code behavior, error codes, permission rules, cache strategy, or transaction boundaries change, update the related comments too. Stale comments that contradict implementation are forbidden.
* **No magic values**: Statuses, types, default sort values, and cache key fragments must use enums or constants.
* **Control method size**: Keep methods focused. Split complex business logic into private methods or domain services.
* **Null handling**: Handle external input, nullable fields, and third-party return values explicitly. Do not rely on accidental NPEs.
* **Collection return**: Return empty collections instead of `null` when no data exists.
* **Time handling**: Use `LocalDateTime` and `LocalDate` consistently. Avoid mixing `Date` and string-based time values.
* **Money and numeric precision**: Use `BigDecimal` for money or exact decimal values. Do not use `float` or `double`.
* **Object conversion**: Entity, DTO, and VO conversion belongs in Converters. Do not scatter setter-based conversion logic inside Controllers.
* **Dependency injection**: Prefer constructor injection over field injection.

---

## 15. API Documentation Rules

* **Every API must be documented**: Both Admin and Portal APIs must have documentation including path, method, request parameters, response fields, and error codes.
* **Field semantics must be explicit**: Enum fields must document available values and meanings.
* **Examples must be complete**: Core APIs should include request and response examples.
* **Changes must be traceable**: Deleted fields, renamed fields, and semantic changes must include change notes and frontend impact assessment.
