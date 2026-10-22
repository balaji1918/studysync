# StudySync â€” Learn Together. Build Together.

A Java web application for college students: ask and answer doubts, find study partners, and build project teams.

**Stack:** Java 21, Spring Boot 3.5, Spring MVC, Spring Security, Spring Data JPA, Thymeleaf, MySQL 8, Flyway, Maven.

## Build status

| Phase | What | Status |
|---|---|---|
| 1 | Foundation: landing page, register, login/logout, profiles, database schema | **Code written** |
| 2 | Explain My Doubt | Implemented and tested against MySQL |
| 3 | Find My Study Partner | Implemented and tested against MySQL |
| 4 | Find My Project Team | Implemented and tested against MySQL |
| 5 | Dashboard sections, notifications, admin | not started |
| 6 | Full test suite, security review, deployment polish | not started |

The three core workflows use the existing V1 database tables. Existing installations need no schema reset or destructive migration.

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

- `DB_PASSWORD` (required) â€” the password you chose above
- `DB_USERNAME` (default `studysync`), `DB_URL` (default points to local MySQL)

**IntelliJ:** Run â†’ Edit Configurations â†’ your Spring Boot config â†’ *Environment variables* â†’ `DB_PASSWORD=your-password`.

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

## Core workflows

- **Explain My Doubt** (`/questions`): ask with subject/topic, filter questions, open a question, post explanations, mark other students' answers helpful once, and resolve your own answered question.
- **Find My Study Partner** (`/partners`): set WANT_TO_LEARN subjects in My profile or search a subject. Matching shows discoverable, active classmates with CAN_HELP subjects. Send a request, open `/study-requests`, accept/decline/cancel, then schedule sessions with meeting details. Both participants see their sessions; other students cannot access them.
- **Find My Project Team** (`/projects`): post ideas, required skills/roles and team capacity; search projects; apply; owners accept/reject applications. Applicants can withdraw pending applications. Accepted students get the workspace, task assignment, due dates and progress. Only members can read/edit tasks. Owners control recruitment.
- Home feature cards, dashboard and authenticated navigation link to these pages. Logging in preserves the requested destination.
- All writes require authentication and CSRF tokens. Values are validated on the server, user content is escaped, and approval/scheduling/task permissions are enforced independently of the UI. Unique database keys prevent active duplicate requests; project row locks protect acceptance capacity.

### Run the workflow integration tests on MySQL without Docker

Create a **dedicated empty test database** first. These tests run Flyway and create temporary test users inside rolled-back transactions. Never point the tests at a production database.

```powershell
$env:WORKFLOW_TEST_DB_URL='jdbc:mysql://localhost:3306/studysync_workflow_test?serverTimezone=UTC'
$env:WORKFLOW_TEST_DB_USERNAME='studysync_test'
$env:WORKFLOW_TEST_DB_PASSWORD='your-test-password'
mvn test
```

`WorkflowIntegrationTest` runs when `WORKFLOW_TEST_DB_URL` is present. Otherwise it is skipped. The original Docker-based auth integration tests still require Docker. The workflow tests exercise all six pages, every write route, subject matching/privacy, helpful votes, approvals, sessions, duplicate requests, team capacity, task permissions, CSRF and login redirects.

### Updating an existing deployment

Build with Java 21 and `mvn clean package`, then deploy `target/studysync-0.1.0.jar` using the existing `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. The existing V1 schema is reused. Back up your deployment database before any release. No deployment platform configuration or production credentials are included in this repository.

Session date input requires an ISO date/time with a UTC offset, such as `2026-11-01T18:00+05:30`. The saved session list displays UTC. Meeting links/locations are stored as coordination details; the app does not provide video calls or live chat.
