# Testing Rules

Applicable scope: the official website Admin system, Portal APIs, media assets, customer leads, permission/security logic, cache synchronization, and database access. Expected stack: Java, Spring Boot, MyBatis Plus, MySQL.

---

## 1. General Principles

* **Testing is a delivery gate**: Core business code must come with tests. Manual verification alone is not enough.
* **Layered testing**: Unit tests, slice tests, integration tests, and API tests cover different risks. One type of test must not replace all others.
* **Business rules first**: Prioritize testing logic that can easily cause production incidents, such as show/hide rules, permission boundaries, cache invalidation, data filtering, and audit logs.
* **Repeatable execution**: Tests must be repeatable locally and in CI. They must not depend on execution order or special local machine data.
* **Real risk first**: Testing is not about meaningless coverage. Prioritize paths that can cause data errors, authorization bypass, content exposure, or lead leakage.

---

## 2. Test Layering Rules

### 2.1 Unit Tests

* **Scope**: Pure business rules inside Services, Converters, enum parsing, utilities, state machines, and validators.
* **Isolate dependencies**: External dependencies must be mocked. Do not connect to real database, cache, object storage, or third-party services.
* **Focus**: Happy path, boundary values, invalid states, exception branches, and null handling.
* **Naming**: Test classes should be named `XxxTest`. Test method names should describe business scenario and expected result.
* **Prohibitions**: Do not duplicate implementation logic just to make the test pass. Do not spend time testing getters, setters, or framework code with no business value.

### 2.2 Web Layer Tests

* **Scope**: Controller parameter validation, route mapping, unified response structure, permission interception, and exception responses.
* **Recommended approach**: Use Spring MVC Test or equivalent tooling to test HTTP behavior.
* **Focus**: Missing required parameters, invalid parameter format, unauthenticated access, forbidden access, resource not found, and success response fields.
* **Isolation rule**: Controller tests should mock Services. Do not mix business logic and database access into Web-layer tests.

### 2.3 Data Access Tests

* **Scope**: Mappers, custom XML SQL, complex query conditions, pagination, sorting, and logical-delete filtering.
* **Focus**: Empty-condition dynamic SQL, unique indexes, state filtering, sort rules, and pagination boundaries.
* **Database requirement**: Lightweight validation is acceptable for ordinary Mapper logic. When MySQL dialect features, `deleted_marker` composite unique indexes, migration scripts, complex SQL, JSON fields, or time functions are involved, verification must run against MySQL or an accepted compatible environment.
* **Prohibition**: Complex SQL must not be verified only through mocking. At least one integration test must cover it.

### 2.4 Integration Tests

* **Scope**: Service + Mapper + database + transaction + cache invalidation + audit log.
* **Focus**: Show/hide, delete, upload bind/unbind, lead submission and follow-up, role change, cache invalidation, and similar cross-component flows.
* **Data isolation**: Every test prepares its own data and rolls back or cleans up afterward. Do not depend on artifacts from other tests.
* **External dependencies**: Object storage, notifications, and third-party statistics should use Fake, Stub, or local simulation by default.

### 2.5 API Regression Tests

* **Scope**: Public Portal APIs, core Admin APIs, and customer form submission APIs.
* **Focus**: Stable response structure, stable error codes, usable pagination, invisible hidden/deleted content, and effective permission control.
* **Contract rule**: Important API field changes must update both API tests and API documentation.

---

## 3. Test Directory and Naming Rules

Recommended structure:

```text
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

* **Place tests by module**: Test code should correspond to production modules for easier risk location.
* **`support`**: Holds test data factories, Fake implementations, assertion helpers, and test constants.
* **Class naming**: Unit tests use `XxxTest`, integration tests use `XxxIntegrationTest`, and Web tests use `XxxControllerTest`.
* **Method naming**: Method names should express `scenario + expected result`, for example `hideProduct_shouldReject_whenAlreadyDeleted`.
* **Test data naming**: Avoid real customer names, phone numbers, and email addresses in tests.

---

## 4. Coverage Scope Rules

The following functionality must have automated test coverage:

* **Login and permissions**: Unauthenticated access, forbidden access, role differences, disabled account, and permission change.
* **Content visibility**: Show, hide, and logical delete. Draft, approval, and scheduled publish need coverage only after the PRD explicitly introduces them.
* **Portal visibility**: Portal APIs must return only visible, non-deleted, publicly accessible content.
* **Navigation menu**: Two-level menu, sorting, hiding, target type, and invalid links.
* **Homepage configuration**: Banner, CTA, KPI blocks, AI capability cards, and cache invalidation.
* **Product module**: Categories, tags, product show/hide, product detail features, and quantified indicators.
* **Case module**: Industry relation, case show/hide, rich text, cover image, and recommended state.
* **News module**: Category filtering, publish-time ordering, pinned items, recommended items, and SEO fields.
* **Lead module**: Form validation, basic anti-abuse, processing status, masked display, single-lead full-detail access for Administrators, and admin-only export restriction.
* **Media module**: File type restriction, size restriction, path generation, metadata persistence, bind, unbind, rich-text media reference lists, and failure cleanup.
* **Audit logs**: Show/hide, delete, export, sensitive-data view, and role change.
* **Cache synchronization**: After Admin updates, related Portal cache must be invalidated only after transaction commit, and TTL plus delayed second-delete must be verified. On transaction rollback, cache must not be invalidated incorrectly or written with dirty data.

---

## 5. Assertion Rules

* **Assertions are mandatory**: Tests must assert returned values, status codes, database state, logs, or side effects. Do not just call methods without verification.
* **Assert business semantics**: Prefer assertions such as "hidden content does not appear in the list" instead of only asserting list length.
* **Exception assertions must be explicit**: Exception tests must assert exception type and key error code, not just "some exception was thrown".
* **Response structure assertions**: API tests must assert unified fields such as `code`, `message`, `data`, and `traceId`.
* **Permission assertions**: Security-related tests must assert access denial. Success-path testing alone is insufficient.
* **Database assertions**: Write-operation tests must assert final database state, not only Service return success.

---

## 6. Test Data Rules

* **Use test data factories**: Complex entities should be created through Factory or Builder utilities instead of handwritten repetitive setup in every test.
* **Keep data minimal**: Each test should create only the data needed for its scenario.
* **No order dependency**: Tests must not share state through execution order.
* **No real privacy data**: Real phone numbers, email addresses, customer names, tokens, or secrets must not appear in test code, SQL, or JSON.
* **Fixed time**: Tests involving submission time, expiration time, or scheduled tasks must use a controllable clock and must not depend on current system time.
* **Deterministic randomness**: Tests involving random IDs, verification codes, or filenames should use predictable generators or assert patterns instead of exact random values.

---

## 7. Database Testing Rules

* **Schema must be rebuildable**: Test database schema must be created through migration scripts or initialization scripts, not manual table creation.
* **Data must be cleanable**: Each integration test should remain isolated through transaction rollback, table cleanup, or an isolated schema.
* **Constraints must be verified**: Unique indexes, non-null constraints, foreign keys, or equivalent business constraints must have at least one test.
* **Logical-delete filtering**: All Portal queries and default Admin list queries must test logical-delete filtering.
* **Pagination boundaries**: Test first page, empty page, oversized `pageSize`, sort fields, and no-data scenarios.
* **MySQL verification boundary**: SQL using MySQL-specific behavior, `deleted_marker` composite unique indexes, and migration scripts must be verified in MySQL or an accepted compatible environment. Not every daily test run is required to start a MySQL container.

---

## 8. Cache Testing Rules

* **Cache-hit tests**: High-frequency Portal APIs should verify that cache hits do not repeatedly query the database, or at least verify result consistency.
* **Cache invalidation tests**: After Admin save, show/hide, delete, or sort changes, tests must verify cache cleanup or rebuild through a post-commit mechanism.
* **Delayed double-delete tests**: After content change, tests should verify both immediate deletion and delayed second-delete are scheduled, and that Portal cache has TTL fallback.
* **Rollback tests**: When a write operation rolls back, cache must not be refreshed with uncommitted data.
* **Null-value cache tests**: Short-lived empty-result cache for missing data should have expiration and correct return behavior verified.
* **Concurrency protection tests**: If hotspot cache rebuild uses locking or async refresh, tests should verify repeated requests do not cause obvious duplicate rebuild storms.

---

## 9. Security Testing Rules

* **Authentication tests**: Admin APIs must cover rejection of unauthenticated access.
* **Authorization tests**: For high-risk endpoints, tests must cover both allowed and denied scenarios for Administrator and unauthenticated or unauthorized callers.
* **Privilege escalation tests**: Scenarios where a user modifies IDs to access other users' or unauthorized resources must be tested.
* **Input attack tests**: SQL injection fragments, XSS fragments, overly long strings, invalid enums, and invalid links must be tested.
* **Upload attack tests**: Forged MIME, double extensions, oversized files, script files, and path traversal filenames must be tested.
* **Sensitive-data tests**: API responses and logs must not contain full passwords, tokens, phone numbers, or email addresses.
* **Lead permission tests**: Administrators may view full contact details for one lead and update follow-up status. Lead export must cover denial for unauthenticated or unauthorized callers, success for Administrators, and export audit logging.

---

## 10. Exception and Boundary Testing Rules

* **Resource not found**: Query, update, or delete of a non-existent resource must return a clear business error.
* **State conflict**: Re-hiding already hidden content, showing already deleted content, recreating deleted unique keys, and unbinding old media after content update must all be tested.
* **Repeated submission**: Form submission, show/hide, delete, and similar operations must consider repeated invocations.
* **Empty collection**: When a list has no data, it must return an empty collection and correct pagination information, not `null`.
* **External failures**: Object storage failure, notification failure, cache failure, and similar scenarios must have tests to ensure main flow behavior matches design.
* **Concurrent conflict**: If core content editing uses optimistic locking, version conflict must be tested.

---

## 11. Mock and Fake Usage Rules

* **Use Mock only to isolate boundaries**: Unit tests may mock Mapper, cache, notification, and object storage dependencies. Do not mock the class under test itself.
* **Prefer Fake for complex dependencies**: Cache, storage, and notification dependencies may provide Fake implementations to reduce brittle over-mocking.
* **Avoid over-verifying implementation detail**: Do not over-assert internal call counts unless the call itself is a business requirement, such as cache invalidation or audit log write.
* **Mock behavior must stay realistic**: Mock return values must follow realistic business constraints. Do not fabricate impossible production data to hide design flaws.

---

## 12. CI and Execution Rules

* **Run before commit**: Before local commit, at least run affected-module unit tests and necessary integration tests.
* **Run before merge**: Before merging to the main branch, run the full test suite or the complete CI-defined test set.
* **Do not ignore failures**: Failed tests must be fixed or explicitly marked as temporarily skipped with reason and recovery time.
* **Do not skip casually**: Do not delete tests, comment them out, or weaken assertions just to merge code.
* **Tier test duration**: Fast unit tests and slower integration tests may be grouped separately, but critical integration tests must not remain unrun for long periods.
* **Keep reports**: CI should keep test reports, coverage reports, and failure logs for troubleshooting.

---

## 13. Coverage Rules

* **Coverage is a floor, not the goal**: Coverage cannot replace test quality. Core business branches must have explicit tests.
* **Higher requirement for core modules**: Permissions, leads, content visibility, cache, audit, and media upload are higher-risk modules and should receive stronger coverage first.
* **New code requirement**: New business logic must add tests at the same time. Bug fixes must include regression tests.
* **Explain coverage drop**: If a merge request causes a clear coverage drop, the reason must be explained or tests must be added.
* **Controlled exclusions**: Configuration classes, startup classes, and pure DTOs may be excluded from coverage metrics, but Service business logic must not be excluded.

---

## 14. Testing Prohibitions

* Do not test only the success path while ignoring exception, permission, and boundary scenarios.
* Do not let tests depend on production database, production object storage, or production third-party services.
* Do not write real customer privacy data, real tokens, or real secrets into tests.
* Do not let API tests assert only HTTP 200 without checking business code and response data.
* Do not verify complex SQL only through mocking.
* Do not let tests depend on execution order or share dirty state.
* Do not weaken business validation, permission validation, or security rules just to make tests pass.
* Do not keep disabled tests for a long time without explanation.

---

## 15. Pre-Submission Testing Checklist

* Does every new or changed business rule have corresponding tests?
* Are success, failure, boundary, forbidden, and resource-not-found scenarios covered?
* For database queries, are logical delete, state filtering, pagination, and sorting tested?
* For content show/hide or delete, are Portal visibility, media unbinding, post-commit cache invalidation, TTL, and delayed second-delete tested?
* For sensitive data, are masking, permission, and audit tested?
* For file uploads, are type, size, path, and failure handling tested?
* Is test data repeatable to build and free of real privacy data?
* Can tests run independently without execution-order dependency?
* Has a regression test been added for bug fixes?
* Have the relevant test suites run locally or in CI?
