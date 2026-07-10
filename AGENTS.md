# AGENTS.md

This file is the collaboration entry point for this repository. Any automated agent, developer, or code reviewer must read this file before modifying the project and must follow the rules under the `rules/` directory.

---

## Project Background

This project is an official website system. The goal is to build a dynamically configurable website backend and public Portal API set.

The system is split into two parts:

* **Admin management side**: used to maintain site settings, navigation, homepage content, AI capability cards, product matrix, industry solutions, featured cases, news, media assets, customer leads, permissions, and audit data.
* **Portal public APIs**: provide stable, cacheable, SEO-friendly public data for the website frontend and receive customer inquiry forms. In production multi-instance deployment, Portal cache should default to Redis or an equivalent shared distributed cache.

For complete business requirements, see [docs/需求说明.md](docs/需求说明.md).

For the project blueprint, see [docs/项目蓝图.md](docs/项目蓝图.md).

For API documentation, see [docs/接口文档.md](docs/接口文档.md). Every new, changed, or removed API must be reflected in that document.

Please update the [docs/重构接口文档.md](docs/重构接口文档.md) for the refactored interface.

---

## Technology Stack

Current backend stack:

* Java 17
* Spring Boot 3.3.6
* MyBatis Plus 3.5.9
* MySQL
* Maven

If object storage, message queues, search, permission frameworks, schedulers, test containers, or other components are introduced later, the related rule documents must be added or updated before large-scale implementation starts. Redis or an equivalent shared cache is the default direction for Portal cache in production multi-instance deployment.

---

## Required Rules

Read and follow these documents before changing code:

* [Backend Development Rules](rules/backend.md)
* [Security Development Rules](rules/security.md)
* [Testing Rules](rules/testing.md)
* [Code Review Rules](rules/review.md)

Priority rules:

* Work related to backend structure, APIs, database, transactions, or cache must follow `rules/backend.md`.
* Work related to authentication, authorization, sensitive data, upload, XSS, SQL injection, audit, or secrets must follow `rules/security.md`.
* Work related to unit tests, integration tests, API tests, test data, or CI gates must follow `rules/testing.md`.
* Work related to code review, change assessment, risk identification, or merge readiness must follow `rules/review.md`.

If rules conflict, resolve them in this order:

1. Security rules take priority.
2. Data consistency and rollback safety take priority.
3. Testing and audit requirements must not be weakened just to move faster.
4. If still unclear, pause implementation first and record the item as pending confirmation.

---

## Working Style

### Before Starting a Task

* Read the requirement document and the relevant rule documents first.
* Confirm the current repository structure first. Do not assume a standard Spring Boot project already exists.
* If changing existing code, inspect the current module state and call chain first.
* If the user already has uncommitted changes, do not overwrite or revert them unless explicitly requested.

### During Implementation

* Organize code by business module first. Do not pile all classes into global directories.
* Controllers must not call Mappers directly and must not contain database queries or stateful business rules. Cross-module orchestration belongs in Application Service.
* Services are responsible for transactions, business validation, visible/hidden rules, audit, and post-commit cache invalidation.
* Mappers are only responsible for data access. Complex SQL must remain readable and testable.
* Public classes, public methods, core business rules, exception branches, permission checks, cache invalidation, post-commit actions, and complex SQL must include necessary comments or Javadoc. When behavior changes, related comments must be updated too.
* APIs must not return Entities directly.
* Admin APIs must require authentication and be restricted to the `Administrator` role.
* Portal APIs must not return hidden or logically deleted content. Drafts, approval states, scheduled publish, and similar lifecycle states may be used only after the PRD explicitly introduces them.
* Features involving customer leads, accounts, permissions, exports, or viewing sensitive data must follow the security rules.

## Runtime and Testing Environment

* Local runtime, integration testing, and environment verification should use Docker by default.
* MySQL, Redis, and other required middleware should be started through Docker or Docker Compose unless the repository documentation explicitly defines another approved path.
* If a change introduces new runtime or test dependencies, update the corresponding Docker setup and documentation together.
* If a task cannot be verified in the Docker-based environment, explain the reason and the residual risk explicitly.

### After Completing a Task

* Run the tests related to the change in the Docker-based environment.
* If tests cannot run, explain the reason and the uncovered risk.
* Check whether API docs, rule files, or the requirement document need updates.
* Any new, changed, or removed API requires an update to [docs/接口文档.md](docs/接口文档.md), including the Controller file path and method name in the corresponding API entry.
* Summarize the actual changes, verification result, and remaining risks.

---

## Directory Conventions

Current documentation and rules layout:

```text
.
├── AGENTS.md
├── README.md
├── docs
│   ├── 项目蓝图.md
│   ├── 架构设计说明.md
│   ├── 数据库设计说明.md
│   ├── 错误码说明.md
│   ├── 需求说明.md
│   └── 接口文档.md
└── rules
    ├── backend.md
    ├── security.md
    ├── testing.md
    └── review.md
```

The backend source tree must use the following structure. For detailed layering responsibilities, follow [Backend Development Rules](rules/backend.md):

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

The test directory should mirror the business modules. See [Testing Rules](rules/testing.md) for details.

After an API is implemented, [docs/接口文档.md](docs/接口文档.md) must record the path, HTTP method, auth requirement, request parameters, response fields, error codes, implementation file path, and method name.

---

## Core Quality Gates

Every business implementation must satisfy these minimum requirements:

* All Admin write operations must have authentication and role checks.
* Critical content operations must have audit logs.
* Content visibility, hiding, and deletion must have explicit state rules.
* Portal queries must filter out hidden and logically deleted data.
* Sensitive data such as customer leads must be masked by default. Viewing full contact info and export are administrator-only by default.
* File uploads must validate type, size, path, and access control. Expensive image processing must be async or background-based. Business updates and deletes must maintain media references and handle unbinding.
* Database writes must account for transaction boundaries and unique constraints. Soft-delete tables must handle unique index conflicts correctly.
* Cache invalidation after content changes must happen after transaction commit, with TTL and delayed second-delete as fallback. Production multi-instance deployment must use distributed or equivalent shared cache.
* Complex business logic, permissions, security, cache, and SQL must have test coverage.
* Production configuration must not contain default passwords, debug switches, or plaintext secrets.

---

## Review Requirements

Code review must follow a risk-first approach by default:

* Check security issues, data consistency issues, authorization bypass, and sensitive data exposure first.
* Then inspect visibility rules, transactions, cache, audit, and testing gaps.
* Finally inspect structure, naming, maintainability, and performance.
* Review conclusions must clearly separate blocking issues from non-blocking suggestions.

Do not do formatting-only review. The core risks in this website backend are accidental content exposure, unauthorized changes, lead leakage, media garbage accumulation, multi-instance cache inconsistency, and missing audit trails.

---

## Prohibited Actions

* Do not implement related functionality without first reading the corresponding rule document.
* Do not leave security, authorization, audit, or testing as indefinite follow-up items.
* Do not expose database Entities directly through APIs.
* Do not treat "logged in" as sufficient for Admin APIs without role-based authorization.
* Do not concatenate user input into SQL.
* Do not log passwords, tokens, full phone numbers, or full email addresses.
* Do not use default accounts, default passwords, or debug configuration in production.
* Do not change core visibility, permission, cache, or data access logic without tests.
* Do not add, change, or remove APIs without updating [docs/接口文档.md](docs/接口文档.md).
* Do not leave API docs without implementation locations. The document must allow quick navigation to the Controller file and method.

---

## Pending Confirmations

The following items still require clarification before formal implementation. The base package `com.company.officialwebsite` has already been created and is not considered pending:

* Runtime versions and deployment parameters for MySQL, Redis, and other middleware, including cache namespace conventions for Redis or equivalent shared cache.
* Docker baseline choice for local development and verification, including whether the repository standard is `docker compose`, service naming rules, mounted directories, and bootstrap commands.
* Whether images, videos, and PDFs should be stored in object storage.
* Permission framework choice, such as Spring Security, Sa-Token, or a custom solution. The current authorization model is based on a single `Administrator` role for the backend.
* Database migration tool choice, such as Flyway or Liquibase.
* For MySQL dialect behavior, migration scripts, and `deleted_marker` composite unique indexes, the test environment must provide MySQL through Docker by default, or another team-approved compatible verification path. This can be Docker Compose, Testcontainers, a standalone MySQL instance, or another approved environment. Not every daily test run must start a container if the team has already defined an equivalent Docker-based baseline.
* Whether CRM, WeCom, DingTalk, email, or online customer service integrations are required.
