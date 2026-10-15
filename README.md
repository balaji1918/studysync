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
| 6 | Full test suite, security review, deployment polish | not started |

The database migration (`V1__create_schema.sql`) already contains **all** tables, so later phases only add Java code and pages.

> Honesty note: this code was written without internet access, so it was **not compiled or run** by its author. Run the steps below. If Maven shows an error, copy the exact message and ask for a fix.

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
