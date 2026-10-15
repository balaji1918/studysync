# Deployment review

The repository implements account registration, BCrypt authentication, logout,
dashboard, profile editing, public-to-authenticated-students profiles with discovery
privacy, subject preferences, and public informational pages. Questions, study partner
matching, project teams, notifications, and admin screens are not implemented yet;
V1 reserves tables for them. No UI or feature implementation was replaced.

## Changes

- Dynamic `PORT` with local fallback 8080; forwarded HTTPS headers support proxy
  redirects and Spring Security HSTS. Production ingress must be a trusted proxy.
- `prod` profile requires database settings and defaults session cookies to Secure.
  Existing HttpOnly, SameSite=Lax, timeout, CSRF, CSP and authorization rules remain.
- Fixed an existing integration-test failure: fresh requests missing CSRF now return
  403, while a genuinely stale requested session still redirects to `/login?expired`.
- Java 21 multi-stage Docker build, non-root runtime, minimal build context excluding
  local configuration and private key files. No credentials are needed during builds.
- Render Docker Web Service and `/login` deployment health check; runtime database values
  are supplied through secure Render environment settings. Local HTTP remains supported.
- Flyway checksum validation and clean protection are explicit. V1 is unchanged;
  Hibernate schema generation remains disabled. Existing data is not migrated or removed
  by this change. Production data import/baseline needs backup and separate review.

## Security review scope

SecurityConfig protects all nonpublic routes, restricts `/admin/**`, disables HTTP
Basic, keeps CSRF and POST logout, and sets CSP/frame restrictions. Registration
assigns STUDENT server-side and stores BCrypt hashes. Profile editing uses the session
user ID; hidden/inactive profiles are not disclosed to other students. Production
error responses suppress exception details. Templates escape displayed user content
with `th:text`; form submissions use Thymeleaf CSRF integration.

This is a deployment review, not a full vulnerability audit. Before broad public use,
address the documented absence of login/registration rate limiting and email
verification. Also review the existing password length validation: BCrypt limits
UTF-8 bytes whereas the form currently limits Java character count, so multibyte
passwords can exceed BCrypt's byte limit. That pre-existing application issue is
outside these deployment changes. Keep Spring Boot and image security patches current.
Use one replica until sessions are shared; application restarts invalidate in-memory
sessions without deleting database records.
