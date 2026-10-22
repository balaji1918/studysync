# Railway workflow release

Railway currently follows `render-deployment`. This change brings the core workflows already merged into `main` to that deployment branch, preserving its Dockerfile, port/proxy settings, database configuration, and expired-session handling. No database migration changes are required.

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
