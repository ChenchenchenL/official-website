# Security Development Rules

Applicable scope: the Admin system, Portal public APIs, media upload service, customer lead data, system configuration, and operations/deployment. Expected stack: Java, Spring Boot, MyBatis Plus, MySQL.

---

## 1. General Principles

* **Deny by default**: Admin APIs, management functions, and sensitive data are inaccessible by default and require explicit authorization.
* **Least privilege**: Admin accounts, APIs, database accounts, and object storage keys must receive only the minimum privilege needed to perform their tasks. The current business model uses a single backend role: `Administrator`.
* **Defense in depth**: Authentication, authorization, input validation, data permissions, audit logs, rate limiting, and alerting must be used together. Do not rely on a single line of defense.
* **Sensitive data protection first**: Phone numbers, email addresses, customer messages, tokens, secrets, passwords, and similar data must each be handled properly in storage, transport, display, and logging.
* **Security events must be traceable**: Login, logout, failed login, permission denial, export, sensitive-data view, configuration change, show/hide, and similar actions must be auditable.

---

## 2. Authentication Rules

* **Admin must require login**: Every `/admin/api/**` endpoint must require authentication, except explicitly whitelisted endpoints such as login, captcha, and health checks.
* **Portal must distinguish public and submission APIs**: Public content APIs may be anonymous. Form submission, file download, statistics reporting, and similar APIs must have rate limiting and input validation.
* **Password storage**: User passwords must be stored with secure hashing such as BCrypt or Argon2. MD5, SHA1, and plaintext are forbidden.
* **Failed login protection**: Repeated login failures must trigger temporary lockout, captcha, or frequency limits to prevent brute force attacks.
* **Session lifetime**: Admin sessions must have an expiration time and must expire automatically after long inactivity.
* **Forced logout**: When an account is disabled, password is reset, or role permissions change, existing sessions should be invalidated immediately or within a short bounded window.
* **Token transport**: Tokens must be transmitted over HTTPS only and must not be passed through URL query strings.
* **Remember-me behavior**: If remember-me is implemented, use short-lived access tokens plus revocable refresh tokens. Do not persist long-lived high-privilege tokens.

---

## 3. Authorization and Permission Control Rules

* **Admin endpoints must be authorized**: Authentication alone is not enough. Every Admin endpoint must check whether the current role is allowed to perform the requested action.
* **Role model**: The current stage defines only `Administrator`. All backend management actions, including content maintenance, system configuration, lead handling, export, and other high-risk functions, are administrator operations.
* **Content delete permissions**: Content hide, logical delete, physical delete, system configuration delete, account delete, audit-log delete, and other dangerous backend actions are administrator-only.
* **Lead permissions**: Viewing full contact details for a single lead, updating processing status and notes, batch export, batch delete, and lead-related permission configuration are administrator-only.
* **Avoid overdesign**: At the current stage, a full many-to-many RBAC model of users, roles, and permissions is not mandatory. If more roles or a permission matrix are introduced later, the PRD and migration design must be updated first.
* **Do not bypass through hardcoded identity**: Business code must not hardcode checks such as username equals `admin` to skip authorization. Stable role codes may be used to express administrator, content editor, and lead follow-up operator.
* **Backend check is mandatory**: The frontend may hide buttons, but the backend must perform role checks independently.
* **Prevent horizontal privilege escalation**: Update, delete, and detail-view endpoints must verify both resource existence and current-user access rights. Do not trust only the ID passed from the frontend.
* **Audit permission changes**: Account enable/disable, password reset, and role assignment must all be audited.

---

## 4. Data Permission and Sensitive Data Rules

* **Lead data is sensitive**: Customer name, phone number, email, company, title, and message content must all be treated as sensitive data.
* **Masked by default**: List views must mask phone numbers and email addresses by default, such as `138****1234` and `a***@example.com`.
* **Authorization for full details**: Only Administrators may view full phone number, email, and message details for a single lead, and the system must record viewer, time, lead ID, and source.
* **Controlled export**: Lead export is administrator-only by default. It must be audited, support scoped filtering, and must not allow unconditional full-table export.
* **Mask logs**: Logs must not print full phone numbers, email addresses, tokens, passwords, verification codes, cookies, or `Authorization` headers.
* **Database field protection**: Passwords, tokens, secrets, verification codes, and similar data must not be stored in plaintext. Sensitive configuration that must be stored should be encrypted or managed through a secret management system.
* **Test data isolation**: Development and test environments must not use production lead data unless it has been desensitized.

---

## 5. Input Validation Rules

* **All external input is untrusted**: HTTP parameters, headers, cookies, upload filenames, rich text content, and third-party callbacks must all be validated.
* **Bean Validation**: Request DTOs must use `@NotNull`, `@NotBlank`, `@Size`, `@Pattern`, and similar annotations for baseline validation.
* **Second-layer validation in Service**: Business rules such as uniqueness, visibility state, and resource ownership must be validated again in the Service layer.
* **Length limits**: Fields such as title, summary, links, messages, rich text, and tags must have maximum lengths to avoid database errors and resource exhaustion attacks.
* **Enum whitelist**: Status, type, jump mode, file type, and similar fields must use enums or whitelist validation. Do not accept arbitrary strings directly into persistence.
* **Pagination limits**: Paginated queries must restrict maximum `pageSize` to prevent oversized requests from overloading the database.
* **Sort-field whitelist**: If the frontend may pass sort fields, map them through an allowlist. Do not concatenate raw sort field names into SQL.

---

## 6. SQL and Database Security Rules

* **No string-concatenated SQL**: Do not concatenate user input into SQL, `Wrapper.last`, or XML `${}` segments.
* **Prefer parameter binding**: In MyBatis XML, user input must use `#{}`. `${}` must not be used for external input.
* **Guard dynamic SQL against empty conditions**: Update, delete, export, and batch operations must prevent empty conditions from turning into full-table operations.
* **Database least privilege**: Application database accounts may only have the required permissions. Production must not use `root` or DBA accounts.
* **Unique constraints as final safety net**: Critical uniqueness rules must be backed by database unique indexes. Soft-delete tables must use the `deleted_marker` pattern so deleted records do not block recreation.
* **Migration script review**: DDL scripts must go through review. Do not casually drop columns, truncate tables, or change field semantics.
* **Slow query governance**: High-frequency Portal queries must have appropriate indexes to avoid database unavailability under traffic spikes or malicious access.

---

## 7. XSS and Rich Text Security Rules

* **Rich text must be sanitized**: Rich text content for news, cases, and standalone pages must be sanitized through an allowlist before storage or before output.
* **Forbid dangerous tags**: `script`, `iframe`, `object`, `embed`, event attributes, `javascript:` links, and similar dangerous content are forbidden by default.
* **Allowlist tags only**: Only tags and attributes required by the business may be allowed, such as paragraph, heading, image, table, link, bold text, and list tags.
* **Link safety**: External links should validate protocol and only allow explicitly permitted schemes such as `http`, `https`, and `mailto`. Links opened in new windows should preferably include `noopener noreferrer`.
* **Frontend escaping**: Plain text fields must be escaped on display and must not be rendered as raw HTML.
* **Isolated Admin preview**: Rich text preview must not bypass sanitization, to prevent stored XSS against Admin users.

---

## 8. File Upload Security Rules

* **Unified upload entry**: All uploads must go through the media module. Business modules must not receive files directly.
* **Double validation of extension and MIME**: File extension, MIME type, and file content signature must all be validated. Do not trust only frontend-provided metadata.
* **No executable files**: JSP, HTML, JS, EXE, SH, BAT, JAR, CLASS, and similar executable or script files are forbidden.
* **File size limits**: Images, videos, and PDFs must each have maximum allowed size and must be rejected when exceeded.
* **Safe filenames**: Final stored filenames must be generated by the server. Do not use the original user filename as the path, to avoid path traversal and file overwrite.
* **Path isolation**: Uploaded files must not be stored in executable directories or template/static rendering directories of the application.
* **Safe image processing**: Image compression and transcoding should be async or background-based. If processing fails, the file must be marked failed and the original validation result retained. Do not let abnormal files pass through because processing failed.
* **Access control**: Public media and Admin-private attachments must have different access strategies. Customer lead attachments must not be publicly accessible.

---

## 9. CSRF, CORS, and Request-Origin Rules

* **Protect Admin write endpoints from CSRF**: If Cookie-based sessions are used, Admin write endpoints must enable CSRF protection or an equivalent mechanism.
* **CORS allowlist**: Cross-origin sources must be configured per environment using an allowlist. Production must not use `*` together with credentials.
* **Method restriction**: Endpoints should expose only the HTTP methods they actually need. Do not expose unnecessary capabilities.
* **Header validation**: Authentication, tracing, and origin headers must have explicit format validation. Do not trust them blindly.
* **Referer is not sufficient alone**: `Referer` and `Origin` may be used as supporting signals, but never as the only security control.

---

## 10. Rate Limiting and Anti-Abuse Rules

* **Login rate limiting**: Login, captcha, SMS, and email sending endpoints must be rate-limited by account, IP, device, or a combination of them.
* **Form submission rate limiting**: Customer inquiry forms must prevent repeated submission and bot abuse.
* **Upload rate limiting**: Upload endpoints must limit request frequency, per-file size, and total volume, to prevent storage exhaustion.
* **Export rate limiting**: Data export should limit frequency and maximum export size. When needed, use async generation and time-limited download.
* **Portal hotspot protection**: High-frequency endpoints such as homepage, navigation, product list, and news must rely on cache and include anti-penetration and anti-breakdown strategy.
* **Record abnormal traffic**: Repeated failures, authorization bypass attempts, invalid parameters, and upload anomalies should be logged for alerting and investigation.

---

## 11. Audit Log Rules

* **Actions that must be audited**: Login success, login failure, logout, account enable/disable, role changes, password reset, content show/hide, content logical delete, system-level delete, viewing full lead contact details, lead export, system configuration change, media delete, and manual cache refresh.
* **Audit fields**: Record operator ID, operator name, IP, user-agent, module, action, target ID, result, operation time, and `traceId`.
* **Change snapshots**: Content changes, permission changes, and system configuration changes should record core before/after field snapshots.
* **Tamper-resistance requirement**: Audit logs must not be modifiable or deletable by ordinary administrators, and administrators should not directly delete audit records either.
* **Query capability**: The backend should support filtering audit logs by time, operator, module, action, target ID, and result.

---

## 12. Configuration and Secret Management Rules

* **Do not commit secrets**: Database passwords, JWT secrets, object storage keys, email passwords, and webhook URLs must not be committed to Git.
* **Use environment variables or config center**: Sensitive configuration should be injected through environment variables, a configuration center, or a secret management service.
* **Environment isolation**: Development, testing, staging, and production must use different databases, caches, object storage buckets, and secrets.
* **Secure defaults**: Production must not enable default accounts, default passwords, debug mode, SQL logging, or detailed error pages.
* **Secret rotation**: Long-lived secrets, tokens, and webhooks should support periodic rotation.
* **Audit config changes**: Changes to system configuration, notification channels, statistics scripts, and upload strategy in the backend must be audited.

---

## 13. Error Handling and Information Leakage Protection

* **Unified error response**: API exceptions must be converted by the global exception handler into a unified response, without exposing stack traces, SQL, server paths, or class names.
* **Authentication errors should be clear but not enumerating**: Login failure messages must not distinguish between account-not-found and wrong-password, to avoid account enumeration.
* **Resource existence handling**: For unauthorized access to a resource, return either forbidden or not-found depending on the scenario, without leaking resource existence unnecessarily.
* **Restrict debug information**: Production must not expose unauthenticated Swagger, sensitive Actuator endpoints, detailed error pages, or SQL logs.
* **Controlled `traceId` exposure**: Responses may include `traceId` for troubleshooting, but must not include internal error details.

---

## 14. Third-Party Integration Security Rules

* **Protect webhooks**: Webhook URLs for WeCom, DingTalk, email, CRM, and similar integrations are sensitive configuration and must not be exposed to the frontend.
* **Verify callback signatures**: Incoming third-party callbacks must verify signature, timestamp, and replay window.
* **Timeouts are mandatory**: Calls to third-party services must set connection timeout, read timeout, and retry limits.
* **Failure isolation**: Third-party notification failure must not keep the main business transaction blocked for a long time. Prefer async jobs or events.
* **Minimal data transfer**: Only send business-required lead fields to third parties. Avoid exposing full sensitive data.

---

## 15. Operations and Deployment Security Rules

* **Protect Admin entrypoint**: The Admin side should restrict access origin or add extra protection such as VPN, IP allowlist, captcha, or two-factor authentication.
* **Tiered health checks**: Public health checks should return only service status and must not expose database, cache, version, host, or similar sensitive details.
* **Secure backups**: Database and media backups should be stored encrypted and protected by download restrictions.
* **Log retention**: Security logs, audit logs, and access logs should have clear retention periods and controlled query permissions.
* **Dependency vulnerability governance**: Regularly scan Maven dependencies, base images, and runtime environments for vulnerabilities, and upgrade high-risk components promptly.

---

## 16. Security Prohibitions

* Do not store passwords, tokens, secrets, or verification codes in plaintext.
* Do not rely on login alone without role-based authorization for Admin endpoints.
* Do not treat hidden frontend buttons as a substitute for backend permission checks.
* Do not concatenate user input into SQL.
* Do not render rich text without sanitization.
* Do not store uploaded files in executable directories.
* Do not expose unauthenticated Swagger, sensitive Actuator endpoints, or detailed error pages in production.
* Do not log `Authorization`, cookies, passwords, full phone numbers, or full email addresses.
* Do not allow unauthorized accounts to export lead data. Lead export is administrator-only by default.
* Do not deploy production with default accounts or default passwords.

---

## 17. Pre-Submission Security Checklist

* Are all Admin endpoints authenticated and authorized under the `Administrator` model?
* Can the current user access only data within the authorized scope?
* Do request DTOs complete baseline validation, and do Services complete business validation?
* Does SQL use parameter binding and avoid `${}` for external input?
* Is rich text sanitized through an allowlist, and is plain text escaped on output?
* Do upload endpoints validate extension, MIME, size, content signature, and storage path?
* Are sensitive fields masked by default? Is single-lead full-detail access by Administrators audited? Is export admin-only and audited?
* Do critical operations write audit logs and change snapshots?
* Is production configuration free of default passwords, debug mode, and sensitive-data leakage?
* Do logs and error responses avoid exposing sensitive data and internal implementation details?
