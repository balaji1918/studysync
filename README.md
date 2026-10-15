# StudySync — Learn Together. Build Together.

A Java web application for college students: ask and answer doubts, find study partners, and build project teams.

**Stack:** Java 21, Spring Boot 3.5, Spring MVC, Spring Security, Spring Data JPA, Thymeleaf, MySQL 8, Flyway, Maven.

## Build status

| Phase | What | Status |
|---|---|---|
| 1 | Foundation: landing page, register, login/logout, profiles, database schema | **Code written** |
| 2 | Explain My Doubt | not started |
| 3 | Find My Study Partner | not started |
| 4 | Find My Project Team | not started |
| 5 | Dashboard sections, notifications, admin | not started |
| 6 | Full test suite, security review, deployment polish | Docker workflow validated locally; Render deployment pending |

The database migration (`V1__create_schema.sql`) already contains **all** tables, so later phases only add Java code and pages.

Validation results for the Render preparation are recorded in `docs/DEPLOYMENT-VALIDATION.md`. Run the checks below on Java 21 before deploying.

## 1. Install the tools (once)

1. **JDK 21** (Temurin from adoptium.net). Check: `java -version`
2. **MySQL 8** Community Server. Remember the root password you set.
3. **IntelliJ IDEA Community** (it includes Maven) or VS Code with the Java extensions.
4. Optional: **Docker Desktop** (only needed for the integration test).

## 2. Create the database (once)

Open the MySQL shell (`mysql -u root -p`) and run:

```sql
CREATE DATABASE studysync CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'studysync'@'localhost' IDENTIFIED BY 'choose-a-strong-password';
GRANT ALL PRIVILEGES ON studysync.* TO 'studysync'@'localhost';
```

Flyway creates all the tables automatically the first time the app starts. The app never deletes data on restart.

## 3. Set your settings (environment variables)

Passwords are **not** stored in the code. Set these before running (see `.env.example`):

- `DB_PASSWORD` (required) — the password you chose above
- `DB_USERNAME` (default `studysync`), `DB_URL` (default points to local MySQL)

**IntelliJ:** Run → Edit Configurations → your Spring Boot config → *Environment variables* → `DB_PASSWORD=your-password`.

**Terminal (Windows PowerShell):** `$env:DB_PASSWORD="your-password"`
**Terminal (Mac/Linux):** `export DB_PASSWORD="your-password"`

## 4. Run

```bash
mvn spring-boot:run
```

Open http://localhost:8080. The database starts empty: click **Create account** to register the first real user.

## 5. Try Phase 1

1. Register at `/register`. Try a short password, a wrong email, and a duplicate email: you should see clear messages.
2. Log in, you land on `/dashboard`.
3. Open **My profile**, add subjects like `Java, SQL`, save. Refresh: data is still there. Restart the app: still there.
4. Untick "Let other students find my profile": another student opening `/students/<your id>` now gets a 404.
5. Log out. Opening `/dashboard` sends you to the login page.

## 6. Tests

```bash
mvn test
```

- Unit tests (registration logic, validation rules, tag parsing) run anywhere.
- `AuthIntegrationTest` starts a temporary MySQL in Docker. Without Docker it is skipped.

## 7. Create the first administrator (safe method)

There is no default admin account. Register normally, then promote that one account directly in MySQL:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';
```

Log out and in again. Only someone with database access can do this; the website has no way to grant ADMIN. (Admin pages arrive in Phase 5.)

## 8. Build a production JAR

```bash
mvn clean package
java -jar target/studysync-0.1.0.jar
```

## 9. Deployment guidance

- Use a managed or self-hosted MySQL 8 and a **separate** database user with a strong password.
- Provide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` as environment variables on the server. Never commit them.
- Put the app behind HTTPS (for example Nginx or Caddy with a free Let's Encrypt certificate) and set `SESSION_COOKIE_SECURE=true`.
- Set `CONTACT_EMAIL` so the Contact page shows a real address.
- Back up the MySQL database regularly.
- Review `privacy` and `terms` pages for your college before inviting real users.

## What is intentionally NOT included yet

- Email delivery and college-email verification (the `users.email_verified` column exists but is always `false`).
- Login rate limiting (planned for Phase 6).
- Live chat or video calls (study coordination will only store agreed details).
- Any AI features.

## Project layout

```
pom.xml
.env.example   .gitignore
docs/ER-DIAGRAM.md
src/main/java/com/studysync/
  StudySyncApplication.java
  config/       SecurityConfig
  security/     StudySyncUserDetails, StudySyncUserDetailsService
  controller/   HomeController, AuthController, DashboardController, ProfileController
  service/      RegistrationService, ProfileService, SubjectService
  repository/   Spring Data JPA interfaces
  entity/       User, StudentProfile, Subject, StudentSubjectPreference + enums
  dto/          RegistrationForm, ProfileForm, PublicProfileView
  exception/    DuplicateEmailException, NotFoundException, InvalidProfileInputException
  util/         TagParser
  validation/ notification/   (filled in later phases)
src/main/resources/
  application.properties
  db/migration/V1__create_schema.sql
  templates/    Thymeleaf pages (fragments, auth, profile, pages, error)
  static/       css/app.css, js/app.js
src/test/java/com/studysync/  unit + integration tests
```

## Render deployment (Java 21 Docker image)

Use a Render **Docker Web Service** and an external managed **MySQL 8** database.
Render does not provide managed MySQL. This setup preserves MySQL and the existing
Flyway migration; do not select Render PostgreSQL or substitute MariaDB. A self-hosted
MySQL private service with a paid persistent disk is possible, but requires you to
operate backups, upgrades and recovery yourself. Managed external MySQL is the default
recommended here.

### 1. Prepare the database

Create a MySQL 8 database with a managed provider, preferably near the Render service's
region. Enable TLS, backups and a database-scoped application user. The user needs DML
and schema DDL privileges for Flyway. Get the hostname, port, database name, username,
password and provider TLS instructions. These are separate from Render credentials.

For an external database, use its reachable TLS endpoint; a private hostname from a
different hosting provider will not be reachable from Render. If the provider uses an
IP allowlist, allow the outbound IP ranges shown for your Render service/region.
Do not open database access to every IP as a shortcut.

### 2. Connect GitHub and create the web service

Review and merge the proposed changes before production deployment. Authorize Render
for `balaji1918/studysync`, then create a **New Web Service** from that repository and
select the reviewed branch. Leave Root Directory empty (repository root), select
**Docker**, and use `./Dockerfile` with context `.`. Choose a region near the database
and a paid Starter plan or larger for a service without free-tier sleep behavior.

The included `render.yaml` is an alternative **Blueprint** setup for the same service.
It selects Starter (billable), asks for database values with `sync: false`, and disables
automatic deployments. Select the reviewed branch when creating the Blueprint.
Creating a service/Blueprint can trigger its initial deployment even with automatic
deployments disabled: do this only after you approve deployment. No service or paid
resource has been created by preparing these files.

### 3. Set environment variables

In Render's Environment settings enter the actual values securely:

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `jdbc:mysql://HOST:PORT/DATABASE?serverTimezone=UTC&sslMode=VERIFY_IDENTITY` |
| `DB_USERNAME` | Your database-scoped application user |
| `DB_PASSWORD` | Its password; enter only in Render settings |
| `SESSION_COOKIE_SECURE` | `true` |
| `PORT` | Render supplies this automatically (normally `10000`); leave unset |
| `CONTACT_EMAIL` | Optional public contact address |

Replace HOST, PORT and DATABASE with your provider's connection details. The URL must
start with `jdbc:mysql://`, not `mysql://`, and must not contain credentials.
`VERIFY_IDENTITY` encrypts traffic and verifies the server certificate and hostname.
Use the provider's certified hostname and TLS instructions. If its CA is not trusted
by the Java image, supply its CA/truststore through supported runtime configuration;
Connector/J supports `trustCertificateKeyStoreUrl`, `trustCertificateKeyStoreType`
and `trustCertificateKeyStorePassword`. Keep any truststore password in secure settings.
Do not disable certificate or hostname verification to resolve a connection failure.
No additional application variable is required by the current code.

### 4. Build, start and health check

No custom build/start command is needed in Docker mode. The Dockerfile runs Maven
with Java 21, then starts `java -jar /app/app.jar` as a non-root user. Spring Boot binds
to all interfaces and uses Render's PORT. Set the health check path to `/login`; the
Blueprint already includes this. Disable Auto-Deploy for the service if creating it
manually, so later deployments remain deliberate. Deploy manually after approval.

Check logs for successful Flyway validation/migration and Spring Boot startup on the
assigned port. Missing credentials, unreachable MySQL or untrusted certificates must
be corrected before treating deployment as successful. Render service health checks
alone are not a complete check of authenticated application behavior.

### 5. Public URL and HTTPS

Render assigns the web service an `onrender.com` URL; there is no separate domain-generation step. Keep public networking enabled and use the HTTPS URL shown in
the service dashboard. A custom domain can be configured under the service's domain
settings with the DNS records Render supplies. Render terminates HTTPS; forwarded
headers preserve HTTPS redirects and Spring Security's HSTS response header. Only
expose this configuration behind a trusted proxy that overwrites forwarded headers.

### 6. Verify deployment

Visit `/`, `/register`, and `/login`. Register a test account, log in, save a profile,
reload it, log out, and confirm `/dashboard` redirects to login. Inspect `JSESSIONID`
for Secure, HttpOnly and SameSite=Lax. Redirects must retain HTTPS. Restart the web
service and confirm the account/profile persists and Flyway does not rerun V1.
Review database backup and restore procedures before inviting real users.

### Existing database safety

Flyway V1 creates the schema only for an empty database. Hibernate schema mutation
is disabled, Flyway clean is explicitly disabled, and migration checksums are validated.
Never edit an already-applied migration: add a new numbered migration instead.
For an existing populated database, back it up and restore/test a copy first, including
`flyway_schema_history` when present. A nonempty schema without Flyway history needs
an explicit schema audit and planned baseline; do **not** enable `baseline-on-migrate`,
run `clean`, delete tables, or repair checksums as a deployment shortcut. A new managed
database does not import your localhost data automatically. Database persistence
belongs in the managed database, not the Render web service's ephemeral filesystem.

### Local development and pre-merge checks

Local defaults remain port 8080 and non-Secure cookies for HTTP. Set DB credentials
in your terminal or VS Code run environment, then run `mvn spring-boot:run`.
Spring Boot does not automatically load `.env`; `.env.example` is a reference only.
Do not use the `prod` profile for local HTTP development.

Before merging run Java 21 + Docker with `mvn clean verify`. Confirm the MySQL
integration tests actually execute rather than skip. Then run `docker build -t studysync .`
and exercise the image against a disposable MySQL 8 instance. Docker image builds skip
unit/integration tests because the MySQL tests need an external Docker daemon; the
pre-merge Maven check is required separately. Secrets must only be supplied at runtime.

Sessions are stored in application memory: use one app instance initially. Restarts
log users out; database records persist. Multiple instances need shared sessions or
an explicitly designed session routing strategy. Registration currently has no email
verification or login rate limiting; plan those before a broad public launch. Study
partner matching, questions, and project collaboration remain future features in this
repository; deployment does not implement them.
