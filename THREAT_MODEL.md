# Krino Threat Model

This document lists the threats considered while building Krino and, for each, the attack
vector, the control implemented (with a reference to the code), and its current status. It
covers the design level; for reporting an actual vulnerability, see the
[security policy](SECURITY.md).

**Status legend:** ✅ implemented · ◐ partial (control exists, known limitation) · ✗ open
(accepted for now or planned).

## System at a glance

A React SPA ([`client/`](client)) talks to a Spring Boot REST API ([`server/`](server))
over JSON. State lives in PostgreSQL; uploaded CVs live in a private MinIO (S3-compatible)
bucket. Authentication is cookie-based JWT (15-minute access token) with rotating,
single-use 30-day refresh tokens. Authorization is role-based (admin, HR manager,
interviewer, candidate) with per-resource ownership checks.

### Assumptions and trust boundaries

- The API is deployed behind a reverse proxy that terminates TLS and **strips or
  overwrites client-supplied `X-Forwarded-*` headers**. Rate-limit keys and audit IPs
  trust these headers (see "IP spoofing" below).
- SPA and API are served from the same registrable domain (`SameSite=Strict` cookies
  assume this).
- PostgreSQL and MinIO are reachable only from the API's network, never exposed publicly.
- Administrators and HR managers are trusted roles; the model defends against anonymous
  users, candidates, and stolen credentials/tokens, not against a malicious admin.

## Authentication and credentials

| Threat | Vector | Control | Status |
|---|---|---|---|
| Brute force / credential stuffing | Scripted guessing against the auth endpoints | Token-bucket rate limiting keyed per client on every auth endpoint (login, register, refresh, forgot/reset-password, verify/resend-verification), with the buckets stored in Redis so limits hold across instances and restarts; a drained bucket refills its full capacity per refill period, so the sustained rate can never exceed the configured burst; see [`RateLimitFilter`](server/src/main/java/com/krino/backend/security/RateLimitFilter.java), [`Bucket4jRateLimiter`](server/src/main/java/com/krino/backend/security/Bucket4jRateLimiter.java), [`RateLimitConfiguration`](server/src/main/java/com/krino/backend/configuration/RateLimitConfiguration.java) | ◐ there is no per-account lockout, so a distributed low-and-slow guess against one account is not throttled by account |
| Offline password cracking | Password hashes read via DB compromise or backup leak | BCrypt at cost factor 12; see [`PasswordEncoderConfiguration`](server/src/main/java/com/krino/backend/configuration/PasswordEncoderConfiguration.java) | ✅ |
| Weak user passwords | User picks a guessable password | Minimum length only (`@Size(min = 8)`); see [`UserRegistrationDTO`](server/src/main/java/com/krino/backend/dto/user/UserRegistrationDTO.java) | ✗ complexity rules or a breached-password (k-anonymity) check are planned |
| User enumeration | Response differences reveal whether an email is registered | Forgot-password and resend-verification return the same response whether or not the account exists; registration returns the same 204 for a taken email (the duplicate attempt is silently ignored and no email is sent); login only reveals the unverified-email state *after* the password was proven correct; see [`PasswordResetService`](server/src/main/java/com/krino/backend/service/PasswordResetService.java), [`EmailVerificationService`](server/src/main/java/com/krino/backend/service/EmailVerificationService.java), [`AuthenticationService`](server/src/main/java/com/krino/backend/service/AuthenticationService.java) | ◐ a response-time difference remains (a duplicate registration skips password hashing and the CV upload), and two concurrent registrations of the same email can still surface as a generic 409 via the unique constraint; both mitigated by per-IP rate limiting |
| Staff onboarding credential leak | Initial staff password exposed in a response or log | 16-char CSPRNG-generated password delivered by email only, never returned by the API; the account is flagged `mustChangePassword`; see [`UserService.createStaff`](server/src/main/java/com/krino/backend/service/UserService.java), [`PasswordGenerator`](server/src/main/java/com/krino/backend/security/PasswordGenerator.java) | ✅ |

## Tokens and session management

| Threat | Vector | Control | Status |
|---|---|---|---|
| JWT algorithm confusion (`alg:none`, RS/HS swap) | Forged token with a manipulated header | Algorithm pinned to HS256 at validation, token `type` claim checked, issuer required, subject must be a UUID, secret length ≥ 32 bytes enforced at startup; see [`JwtService`](server/src/main/java/com/krino/backend/service/JwtService.java) | ✅ |
| Refresh-token theft via DB read | Attacker dumps the `refresh_tokens` table | Tokens are 512-bit `SecureRandom` values stored only as HMAC-SHA256 hashes keyed with a secret *separate* from the JWT secret, compared in constant time (`MessageDigest.isEqual`); a DB read alone cannot forge or reuse a token; see [`TokenHasher`](server/src/main/java/com/krino/backend/security/TokenHasher.java) | ✅ |
| Refresh-token theft via cookie + replay | Stolen refresh cookie replayed after the legitimate client rotated it | Single-use rotation under a pessimistic row lock; replaying an already-consumed token is treated as theft: the user's **entire session family is revoked** (in its own transaction, so the rejection can't roll it back) and a security event is logged; see [`AuthenticationService.refresh`](server/src/main/java/com/krino/backend/service/AuthenticationService.java), [`RefreshTokenService.handleCompromisedToken`](server/src/main/java/com/krino/backend/service/RefreshTokenService.java) | ✅ |
| Cookie theft via script or cross-site request | XSS or third-party page reading/sending auth cookies | `HttpOnly` + `Secure` + `SameSite=Strict` on both cookies; the refresh cookie is path-scoped to `/api/auth` so it never rides along on ordinary API calls; see [`CookieUtilities`](server/src/main/java/com/krino/backend/utility/CookieUtilities.java) | ✅ |
| Access-token replay after logout | Stolen access cookie used during its remaining lifetime | Short 15-minute TTL bounds the window; logout revokes the refresh token and clears cookies, but there is no access-token denylist | ◐ denylist (Redis) planned together with horizontal scaling |
| Stale token accumulation | Expired/used tokens pile up as attack surface and noise | Scheduled cleanup purges expired and revoked/used tokens; see [`TokenCleanupJob`](server/src/main/java/com/krino/backend/service/TokenCleanupJob.java) | ✅ |

## Account lifecycle

| Threat | Vector | Control | Status |
|---|---|---|---|
| Acting from an unproven email address | Logging in before the inbox is verified | Login is blocked until the emailed link is used; verification tokens are single-use, HMAC-hashed, expire in 24 h, and reissuing invalidates earlier ones; see [`EmailVerificationService`](server/src/main/java/com/krino/backend/service/EmailVerificationService.java), [`EmailVerificationTokenService`](server/src/main/java/com/krino/backend/service/EmailVerificationTokenService.java) | ✅ |
| Email change to an unproven address | Verified account switches its email to one the owner never proved | Any email change drops the verified flag and emails a fresh link to the new address; existing sessions survive (so a typo can be corrected) but the next password login requires re-verification; see [`UserService`](server/src/main/java/com/krino/backend/service/UserService.java) | ✅ |
| Password-reset link abuse | Guessed, intercepted, or reused reset link | Single-use HMAC-hashed token with a 30-minute window; request endpoint responds identically for unknown emails; see [`PasswordResetTokenService`](server/src/main/java/com/krino/backend/service/PasswordResetTokenService.java) | ✅ |
| Deactivated account keeps working | User continues after an admin disables the account | Approval gate enforced at authentication; deactivation also deletes the user's refresh tokens, so sessions die within the access token's 15-minute TTL; see [`UserService.setApproval`](server/src/main/java/com/krino/backend/service/UserService.java) | ✅ |

## Authorization

| Threat | Vector | Control | Status |
|---|---|---|---|
| IDOR (object references) | Enumerating sequential IDs in URLs | The API exposes only random UUID `publicId`s; internal sequential `Long` IDs never leave the server; see the entities under [`server/src/main/java/com/krino/backend/entity`](server/src/main/java/com/krino/backend/entity) | ✅ |
| Missing function-level access control | Calling privileged endpoints directly | Two layers: coarse `@PreAuthorize` role checks on every controller **plus** service-level ownership checks (e.g. `requireCurrentUserOrAnyRole`, application-owner checks) so a forgotten annotation alone doesn't open a hole; see [`SecurityUtilities`](server/src/main/java/com/krino/backend/utility/SecurityUtilities.java) | ✅ |
| Mass assignment / privilege escalation | Posting `roles`, `approved`, `emailVerified`, or `status` fields in a request body | DTO→entity mapping is explicit MapStruct with security-relevant fields ignored ([`UserMapper`](server/src/main/java/com/krino/backend/mapper/UserMapper.java)); candidates cannot change application status or re-point an application to another job; see [`ApplicationService`](server/src/main/java/com/krino/backend/service/ApplicationService.java) | ✅ |

## Input handling and uploads

| Threat | Vector | Control | Status |
|---|---|---|---|
| SQL injection | Crafted request parameters reaching queries | Spring Data JPA with parameterized queries throughout; no string-built SQL; see [`server/src/main/java/com/krino/backend/repository`](server/src/main/java/com/krino/backend/repository) | ✅ |
| Malicious file upload | Non-PDF content behind a PDF label | `%PDF-` magic-byte check (not just `Content-Type`) plus a size cap; see [`CvStorageService`](server/src/main/java/com/krino/backend/service/CvStorageService.java) | ✅ |
| Path traversal via filenames | User-controlled names becoming storage paths | Object keys are server-generated UUIDs; the original filename is stored as display metadata only; see [`CvStorageService`](server/src/main/java/com/krino/backend/service/CvStorageService.java) | ✅ |
| PDF-borne script execution | Viewer executing active content from an uploaded CV | Downloads are forced as `Content-Disposition: attachment`, never rendered inline; no malware/deep-content scanning yet | ◐ ClamAV (or equivalent) planned if real CVs are ever handled |
| Pagination/sort abuse | `?sort=<arbitrary property>` causing errors, data exposure or expensive queries | Each paged endpoint runs its `Pageable` through a per-entity [`SortWhitelist`](server/src/main/java/com/krino/backend/utility/SortWhitelist.java) of allowed columns; anything else (sensitive fields, joined relations, unknown names) is rejected with a 400 before it reaches the query, never a 500 or a table scan | ✅ |
| Malformed input | Invalid bodies reaching business logic | Bean validation on create *and* PATCH DTOs | ✅ |

## Web / browser layer

| Threat | Vector | Control | Status |
|---|---|---|---|
| CSRF | Cross-origin form/fetch firing state-changing requests with ambient cookies | Cookie-based CSRF token with the SPA deferred-token pattern (client primes it via `/api/auth/csrf`, echoes the `X-XSRF-TOKEN` header), with `SameSite=Strict` as a second layer; see [`SecurityConfiguration`](server/src/main/java/com/krino/backend/configuration/SecurityConfiguration.java), [`api.ts`](client/src/shared/services/api.ts) | ✅ |
| XSS | Injected markup executing in the SPA | React auto-escaping, JSON-only API (no server-rendered HTML), file downloads as attachment; no `Content-Security-Policy` header yet | ◐ CSP / Referrer-Policy / Permissions-Policy headers planned |
| Clickjacking / MIME sniffing | Framing the app; content-type confusion | Spring Security defaults: `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff` | ✅ |
| Cross-origin API abuse | Unexpected origins calling the API from browsers | Explicit CORS allow-list from configuration, validated at startup in production | ✅ |

## Secrets and configuration

| Threat | Vector | Control | Status |
|---|---|---|---|
| Secrets committed to the repo | `.env` or keys in git history | Secrets live in gitignored `.env` files with placeholder `.env.example`s; JWT and token-HMAC secrets must differ and be ≥ 32 bytes; gitleaks scans the full git history in CI on every push and PR (see [`ci.yml`](.github/workflows/ci.yml)) | ✅ |
| Insecure production deploy | Dev settings (insecure cookies, auto-DDL, default creds, log-only mail) reaching prod | Fail-fast startup validator refuses to boot on insecure prod config: secure cookies required, unsafe `ddl-auto` rejected, blank or shared secrets rejected, default MinIO credentials rejected, CORS origins validated; see [`ProductionConfigurationValidator`](server/src/main/java/com/krino/backend/configuration/ProductionConfigurationValidator.java) | ✅ |
| Management-endpoint exposure | Actuator leaking internals | Only `/actuator/health` is exposed; everything else requires auth/is disabled; see [`SecurityConfiguration`](server/src/main/java/com/krino/backend/configuration/SecurityConfiguration.java) | ✅ |
| API-docs exposure | Swagger UI reachable in production | springdoc/Swagger UI enabled in dev, disabled in prod unless explicitly opted in; see [`application-prod.yaml`](server/src/main/resources/application-prod.yaml) | ✅ |

## Infrastructure and operations

| Threat | Vector | Control | Status |
|---|---|---|---|
| Client-IP spoofing | Forged `X-Forwarded-For` poisoning rate-limit keys and audit logs | Forwarded headers are honoured (`forward-headers-strategy: framework`); correctness depends on the reverse proxy stripping client-supplied `X-Forwarded-*` (a documented trust-boundary assumption) | ◐ enforce at the proxy; a trusted-proxy allow-list in the app is a possible hardening |
| Race conditions | Double-booking an interview slot; concurrent token use; lost updates | Booking uniqueness enforced at the database (`UNIQUE (slot_id)` on interviews), refresh-token consumption under a pessimistic lock, `@Version` optimistic locking on entities; see [`V1__initial_schema.sql`](server/src/main/resources/db/migration/V1__initial_schema.sql) | ✅ |
| PII exposure in logs | Emails logged at info level across auth flows | none yet | ✗ masking/hashing planned |
| Horizontal-scaling gaps | No shared access-token denylist across replicas | Rate-limit buckets are shared via Redis (see [`RateLimitConfiguration`](server/src/main/java/com/krino/backend/configuration/RateLimitConfiguration.java)); refresh-token state lives in PostgreSQL, so rotation and revocation already hold across replicas | ◐ access-token denylist planned before scaling out; the scheduled token cleanup would also need a distributed lock (e.g. ShedLock) to avoid double runs |
| Vulnerable dependencies / supply chain | Known CVEs in libraries or the Docker image | CI builds and tests every push; Dependabot raises weekly update PRs for Maven, npm, Docker base images and GitHub Actions (see [`dependabot.yml`](.github/dependabot.yml)); CodeQL static analysis runs on every push/PR and weekly (see [`codeql.yml`](.github/workflows/codeql.yml)) | ◐ container-image CVE scanning (Trivy or equivalent) still planned |

## Verification

The controls above are exercised by integration tests that boot the real stack
(Testcontainers PostgreSQL + MinIO, Flyway migrations, `ddl-auto: validate`): refresh-token
rotation and reuse-revocation, email-verification and email-change flows, per-role access
denials, rate limiting, and upload validation; see
[`server/src/test/java/com/krino/backend/controller`](server/src/test/java/com/krino/backend/controller).

Known gaps are deliberately listed with an open status above rather than omitted; they are
the current backlog, roughly in the order they would matter for a production deployment.
