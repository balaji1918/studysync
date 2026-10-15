# Deployment preparation validation

Validated in the cloud workspace with Java 21, Maven 3.9.11, Docker, and disposable
MySQL 8.0.46. No existing or production database was used.

- `mvn verify`: **19 tests passed**, zero failures/errors/skips; executable Spring Boot
  JAR packaged successfully. This included eight MySQL authentication integration tests.
- After adding a stale-session regression, `mvn -Dtest=AuthIntegrationTest test`:
  **9 passed**, zero failures/errors/skips. The other eleven tests were unchanged.
- Flyway V1 successfully initialized fresh MySQL schemas. The migration file is unchanged.
- The production-profile JAR started with `PORT=18080`, required DB variables, and
  `sslMode=REQUIRED` against disposable MySQL.
- Final Docker image built successfully and its entrypoint is `java -jar /app/app.jar`,
  running as `studysync` rather than root.
- HTTP smoke checks against the final non-root Docker image passed with the `prod`
  profile, `PORT=18080`, Secure cookies and encrypted MySQL: registration, login,
  dashboard, profile editing, saved subjects, profile persistence after application
  restart, login after restart, logout, HTTPS redirect, HSTS, and missing-CSRF rejection.
  Cookie attributes included Secure, HttpOnly and SameSite=Lax. Flyway validated the
  existing V1 schema on restart without recreating it. Temporary containers were removed.
- `git diff --check` passed; `.env`, `.env.production`, build outputs and private keys
  are ignored. Docker context excludes local credentials and configuration.

The cloud machine initially had only a Java 21 runtime, not its compiler. A verified
Temurin 21 Docker image supplied the full JDK for Maven. Cloud Maven also required
explicit proxy settings and the host's trusted Java CA store. The ordinary Docker
build cannot resolve external hosts in this sandbox, so image validation used a
**temporary Dockerfile outside the repository**, adding only build-time secret mounts
for those Maven proxy settings and trusted CAs, plus a proxy hostname mapping.
TLS verification remained enabled. The committed Dockerfile contains no
cloud-specific proxy configuration. An unmodified build on Render is still to be
verified after deployment approval.

No Render deployment or access to an external managed MySQL service has occurred. These local checks do
not verify your future Render/database credentials, networking, TLS certificate configuration,
public domain, database import, or backups. Follow the README deployment steps after
review and approval.

## Render configuration update

Replaced Railway configuration with a Render Docker Web Service Blueprint and external
managed MySQL instructions. Application behavior and Docker build/start commands are
unchanged; only provider comments, documentation and deployment metadata changed.
The prior Maven and container results above remain local evidence, not Render tests.
The local smoke database used `sslMode=REQUIRED`; the external production database
URL now recommends `VERIFY_IDENTITY`, which must be validated with the provider's
real hostname and CA. No external database credentials are available to test that.

Render Blueprint YAML parsing and local configuration checks passed: Docker paths,
health path, manual deployment setting, production profile and secret placeholders.
This is not Render server-side Blueprint validation. Render documentation requests
were blocked by this cloud environment's network policy (403); provider-side validation
and deployment remain unexecuted. No Maven rerun was needed for this provider switch:
Java, security logic, Docker commands and functional property values did not change.
