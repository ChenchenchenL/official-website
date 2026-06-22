# Code Review Rules

Applicable scope: the official website Admin system, Portal APIs, management APIs, database migration, media upload, permission audit, cache, and deployment configuration. This document defines code review focus, blocking criteria, and output format so that changes remain aligned with the requirement document, backend rules, and security rules.

---

## 1. General Principles

* **Read the requirement before the implementation**: Review must first confirm whether the code satisfies the business flow described in `docs/需求说明.md`, not only whether the code style looks acceptable.
* **Risk first**: Prioritize problems that may cause data errors, authorization bypass, cache inconsistency, production failures, security vulnerabilities, or API breakage.
* **Evidence must be explicit**: Every finding must point to concrete file, line number, trigger scenario, and impact. Avoid purely subjective comments.
* **Feedback must be actionable**: Review comments must state the expected behavior or repair direction, not just "there is a problem here".
* **Blocking criteria must be clear**: Problems involving security, data consistency, permissions, audit, transactions, cache, or API compatibility must block merge until fixed.
* **Respect scope boundaries**: Review should not demand unrelated refactoring and should not reject an implementation that already follows project rules just because of personal preference.

---

## 2. Review Prerequisites

* **Change scope**: Confirm which modules, APIs, schema definitions, configurations, and tests are changed in this submission.
* **Requirement mapping**: Confirm each major change maps back to a requirement item, bug fix, or explicit technical task.
* **Submission completeness**: Confirm whether code, database migration, API docs, configuration examples, and tests are committed together.
* **Local state**: Confirm that temporary files, build artifacts, IDE files, log files, and sensitive configuration are not accidentally committed.
* **Dependency changes**: If Maven, frontend, or operations dependencies are added, their purpose, license risk, and security risk must be explained.
* **Compatibility impact**: Confirm whether existing Admin APIs, Portal APIs, stored data, cache structures, or deployment configuration are affected.

---

## 3. Business Logic Review

* **Flow completeness**: Business flows such as create, edit, show, hide, delete, and reorder must cover both happy path and abnormal path.
* **Legal states**: Visibility states and deletion states must use explicit enums or constants and must validate legal transitions. Approval or scheduled-publish states are reviewed only after the PRD explicitly introduces them.
* **Uniqueness validation**: Unique rules such as menu path, page path, category code, and product name must have Service-layer validation plus database unique-index fallback. Soft-delete tables must use `(business_key, deleted_marker)` composite unique indexes.
* **Portal filtering**: Portal queries must not return hidden, logically deleted, or unauthorized content. Draft, approval, and scheduled publish are checked only when the PRD introduces them.
* **Stable sorting**: Frontend display order must rely on `sort_order` or an equivalent field, with deterministic sorting behavior.
* **Empty data handling**: No-data scenarios must return empty collection, empty object, or explicit default value. Do not return `null` and make the frontend fail.
* **Multi-value input**: Feature lists, tag groups, quantified indicators, and similar multi-value inputs must validate count, length, blank items, and duplicates.
* **Rich text content**: Rich text in cases, news, and similar modules must be sanitized through an allowlist. Plain text must not be rendered as HTML. Rich text media binding must rely on media ID lists or reference lists, not only HTML parsing.

---

## 4. API Review

* **Path conventions**: Admin APIs must use `/admin/api/**`. Portal APIs must use `/portal/api/**`.
* **Unified response**: APIs must return a unified response structure containing business status, message, data, and `traceId`.
* **DTO/VO isolation**: Controllers must not directly receive or return Entity objects, and must not directly return `Map`, raw strings, or database objects.
* **Parameter validation**: Request DTOs must use Bean Validation for required fields, length, format, enum values, and collection size.
* **Pagination limits**: List APIs must be paginated, use `pageNo` and `pageSize`, and enforce maximum `pageSize`.
* **Error semantics**: Business errors must return stable error codes and understandable messages without exposing SQL, stack trace, class name, or server path.
* **API compatibility**: Existing fields must not be removed, renamed, or changed semantically without care. If breaking change is unavoidable, there must be migration notes.
* **Documentation sync**: New or changed APIs must update API documentation with request parameters, response fields, enum meanings, and error codes.

---

## 5. Layering and Code Structure Review

* **Controller responsibility**: Controllers handle HTTP input, annotation validation, and invocation of Services or Application Services. They must not contain database queries or stateful business rules. Cross-module aggregation belongs in the application layer.
* **Service responsibility**: Services handle business rules, transaction boundaries, show/hide validation, audit logs, and post-commit cache invalidation.
* **Mapper responsibility**: Mappers handle data access only and must not carry business decision logic.
* **Module boundaries**: Direct cross-module Mapper access is forbidden. Module interaction should go through Services, query services, Application Services, or events.
* **Centralized conversion**: Entity, DTO, and VO conversion must stay in Converters or other explicit conversion components.
* **Constants and enums**: Statuses, types, cache keys, role codes, and default sort values must not use magic values.
* **Method complexity**: Overlong methods, deep nesting, and duplicated logic should be split, but readability must not be sacrificed for formal abstraction.
* **Dependency injection**: Prefer constructor injection over field injection and hidden dependencies.

---

## 6. Data and Transaction Review

* **Transaction boundary**: Operations involving multi-table writes, show/hide, or audit logs must declare transactions at the Service layer.
* **Post-commit actions**: Cache invalidation, message notification, and external system calls should run through `afterCommit`, transactional event listeners, or equivalent mechanisms to avoid side effects on rollback.
* **Soft delete**: Content-oriented data must use logical delete. Portal queries must query only active data where `deleted_marker = 0` or explicitly filter out deleted data. Review must verify that unique indexes use the `deleted_marker` pattern.
* **Concurrent editing**: Core content editing should use `version`, updated-time validation, or another mechanism to prevent late writes from overwriting earlier commits.
* **SQL safety**: Concatenating external input into SQL, XML `${}`, or `Wrapper.last` is forbidden.
* **Query performance**: High-frequency Portal queries and Admin list filters/sorting fields must have suitable indexes.
* **Batch operations**: Batch update, delete, and export must limit ID count or filter scope to avoid full-table operations.
* **Migration scripts**: DDL must be reviewable and repeatable. It must not drop fields, wipe data, or change historical field semantics without explanation.

---

## 7. Permission and Security Review

* **Admin authentication**: Except for whitelisted endpoints such as login, captcha, and health checks, every `/admin/api/**` endpoint must require authentication.
* **Role authorization**: Admin endpoints must authorize under the three-role model of `Administrator`, `Content Editor`, and `Lead Follow-up Operator`. Content Editors may logically delete content within their scope. Account management, system configuration, physical delete, batch dangerous delete, export, and similar high-risk actions are administrator-only by default. Lead Follow-up Operators may view single-lead full contact details and update follow-up data.
* **Resource authorization**: Detail, update, delete, and export endpoints must verify both resource existence and current-user permission.
* **Sensitive data**: Phone numbers, email addresses, customer messages, tokens, secrets, and passwords must not be exposed in plaintext in responses or logs.
* **Password safety**: Passwords must use BCrypt, Argon2, or another secure hash algorithm. MD5, SHA1, and plaintext are forbidden.
* **Upload safety**: Upload must go through the media module and validate extension, MIME, size, content signature, and storage path. Content update and delete must maintain media references and handle unbinding.
* **XSS protection**: Rich text must be sanitized. Plain text must be escaped. External link protocols must be validated through an allowlist.
* **Rate limiting**: Login, form submission, upload, export, and high-frequency Portal endpoints must have rate limiting or cache protection.
* **CORS/CSRF**: Production CORS must not use dangerous wildcard configuration. Cookie-session write endpoints must have CSRF protection or equivalent.

---

## 8. Audit and Traceability Review

* **Audit critical actions**: Content show/hide, content logical delete, system-level delete, configuration change, role change, full lead contact view, lead export, and manual cache refresh must all record audit logs.
* **Complete audit fields**: Audit logs must include operator, IP, user-agent, module, action, target ID, result, time, and `traceId`.
* **Change snapshots**: Content changes, permission changes, and system configuration changes must record core before/after field snapshots.
* **Failure traceability**: Failed login, permission denial, upload failure, cache rebuild failure, and third-party notification failure must have logs that can be traced.
* **Log masking**: Logs must not print `Authorization`, cookies, passwords, verification codes, full phone numbers, full email addresses, or secrets.

---

## 9. Cache Review

* **Explicit cache targets**: High-frequency Portal reads such as global configuration, navigation, homepage data, product categories, and case recommendations may be cached.
* **Cache key conventions**: Cache keys must use a unified prefix and module name, such as `official:portal:home`.
* **Invalidate after change**: After Admin save, show/hide, delete, or sort changes, the related Portal cache must be cleaned up.
* **Invalidate after commit**: Cache cleanup must happen after database transaction commit. Direct deletion at the end of the transactional method is forbidden.
* **Delayed double-delete and TTL**: Portal cache must have TTL. After content change, cache should be deleted immediately and a delayed second delete should be scheduled to reduce stale-value refill risk.
* **Penetration and breakdown protection**: Hot endpoints should consider empty-result cache, mutex rebuild, or async refresh to avoid database overload under concurrency.
* **Data consistency**: Review must confirm that production multi-instance deployment uses distributed or equivalent shared cache, and that cache response structures stay consistent with database fields and Portal VOs.

---

## 10. Testing Review

* **Unit tests**: Core Service business rules, show/hide logic, uniqueness validation, and role decisions should have unit tests.
* **API tests**: Critical Admin and Portal APIs should cover success, invalid parameter, forbidden, resource-not-found, and hidden/deleted-state scenarios.
* **Security tests**: Upload, rich text, privilege escalation, SQL injection risk, and sensitive-data masking must have automated tests or explicit validation evidence.
* **Cache tests**: Portal cache invalidation through post-commit mechanisms, with TTL and delayed second-delete, must have tests or reproducible validation steps.
* **Migration tests**: Database migration scripts must be validated against both empty schema and historical-data scenarios.
* **Regression scope**: If common components, authentication/authorization, global exception handling, response structure, or cache components are changed, the regression scope must expand.

---

## 11. Merge-Blocking Problems

The following problems must be fixed before merge:

* Admin write endpoints missing authentication or role authorization.
* Critical write operations missing transaction, audit log, or post-commit cache invalidation.
* Portal returning hidden, deleted, or unauthorized data.
* Controller directly calling Mapper, directly returning Entity, or placing cross-module database aggregation inside Controller.
* SQL concatenating external input and creating injection risk.
* Rich text stored or rendered without sanitization.
* Upload endpoints bypassing the media module or accepting dangerous file types.
* Plaintext storage or logging of passwords, tokens, secrets, full phone numbers, or full email addresses.
* Database migration that may lose data without explanation and rollback strategy.
* Breaking existing API compatibility without versioning, migration, or frontend coordination plan.
* Batch update, delete, or export with risk of unconditional full-table operation.
* Core business changes with no tests and no reliable manual proof of correctness.

---

## 12. Review Output Format

Review results should list findings first, ordered by severity:

```text
[Blocking] file-path:line-number
Issue: describe the concrete defect.
Impact: describe the business, security, or stability consequence.
Suggestion: describe the expected repair direction.

[Suggestion] file-path:line-number
Issue: describe the maintainability, performance, or readability concern.
Suggestion: describe an optional improvement direction.
```

Severity definitions:

* **Blocking**: security vulnerability, data error, authorization bypass, transaction inconsistency, API breakage, or production-failure risk. Must be fixed.
* **Important**: likely boundary-case failure, performance issue, or clear maintainability degradation. Should be fixed now or scheduled explicitly.
* **Suggestion**: style, naming, local readability, or lightweight optimization. Does not block merge.

---

## 13. Review Prohibitions

* Do not write only `LGTM` without stating what scope was reviewed.
* Do not demand unrelated refactoring based on personal preference.
* Do not force the submitter to fix historical problems outside the current change, unless the current change amplifies the problem.
* Do not ignore permissions, audit, cache, transactions, and data migration issues in favor of code style only.
* Do not review only the normal path while ignoring exception path, concurrency path, and authorization-bypass path.
* Do not paste sensitive configuration, tokens, passwords, or real customer data into review comments.

---

## 14. Review Completion Checklist

* Is the change confirmed to match the requirement document and business flow?
* Have Admin API authentication and `Administrator` / `Content Editor` / `Lead Follow-up Operator` authorization been checked?
* Have Portal queries been checked to ensure they return only publicly visible data?
* Have layering responsibilities among Controller, Application Service, Service, and Mapper been checked?
* Have DTO/VO isolation and unified response structure been checked?
* Have parameter validation, business validation, `deleted_marker` soft delete, and unique-index fallback been checked?
* Have transaction boundaries, audit logs, post-commit cache invalidation via `afterCommit` or equivalent, TTL, and delayed second-delete been checked?
* Have SQL injection, XSS, upload, and sensitive-data exposure risks been checked?
* Have database migration scripts and indexes been checked for reasonableness?
* Have hotspot cache strategy, anti-penetration, anti-breakdown, TTL, delayed double-delete, and multi-instance cache consistency been checked?
* Have tests covered success path, failure path, and permission boundaries?
* Have API docs, configuration examples, and deployment impacts been updated accordingly?
