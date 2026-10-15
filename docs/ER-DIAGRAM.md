# StudySync database (ER diagram)

Rendered automatically by GitHub and by IntelliJ's Markdown preview (Mermaid).

```mermaid
erDiagram
    USERS ||--|| STUDENT_PROFILE : has
    USERS ||--o{ STUDENT_SUBJECT_PREFERENCE : sets
    SUBJECT ||--o{ STUDENT_SUBJECT_PREFERENCE : "is chosen in"
    USERS ||--o{ QUESTION : asks
    SUBJECT ||--o{ QUESTION : "is about"
    QUESTION ||--o{ ANSWER : has
    USERS ||--o{ ANSWER : writes
    ANSWER ||--o{ ANSWER_VOTE : receives
    USERS ||--o{ ANSWER_VOTE : casts
    USERS ||--o{ STUDY_REQUEST : "sends / receives"
    STUDY_REQUEST ||--o{ STUDY_REQUEST_SUBJECT : covers
    SUBJECT ||--o{ STUDY_REQUEST_SUBJECT : "is covered in"
    STUDY_REQUEST ||--o{ STUDY_SESSION : schedules
    USERS ||--o{ PROJECT : owns
    PROJECT ||--o{ PROJECT_SKILL : requires
    PROJECT ||--o{ PROJECT_JOIN_REQUEST : receives
    USERS ||--o{ PROJECT_JOIN_REQUEST : submits
    PROJECT ||--o{ PROJECT_MEMBERSHIP : has
    USERS ||--o{ PROJECT_MEMBERSHIP : joins
    PROJECT ||--o{ PROJECT_TASK : contains
    USERS ||--o{ PROJECT_TASK : "is assigned"
    USERS ||--o{ NOTIFICATION : gets
    USERS ||--o{ CONTENT_REPORT : files

    USERS { bigint id PK
            string email UK
            string password_hash
            string role
            string status }
    STUDENT_PROFILE { bigint id PK
            bigint user_id FK
            string full_name
            string college
            string department
            int year_of_study
            boolean discoverable }
    SUBJECT { bigint id PK
            string name
            string name_key UK }
    STUDENT_SUBJECT_PREFERENCE { bigint id PK
            bigint user_id FK
            bigint subject_id FK
            string preference_type }
    QUESTION { bigint id PK
            bigint author_id FK
            bigint subject_id FK
            string title
            string status }
    ANSWER { bigint id PK
            bigint question_id FK
            bigint author_id FK }
    ANSWER_VOTE { bigint id PK
            bigint answer_id FK
            bigint user_id FK }
    STUDY_REQUEST { bigint id PK
            bigint sender_id FK
            bigint recipient_id FK
            string status }
    STUDY_REQUEST_SUBJECT { bigint request_id FK
            bigint subject_id FK }
    STUDY_SESSION { bigint id PK
            bigint request_id FK
            datetime scheduled_at }
    PROJECT { bigint id PK
            bigint owner_id FK
            string title
            int open_positions
            string status }
    PROJECT_SKILL { bigint id PK
            bigint project_id FK
            string skill_name }
    PROJECT_JOIN_REQUEST { bigint id PK
            bigint project_id FK
            bigint applicant_id FK
            string status }
    PROJECT_MEMBERSHIP { bigint id PK
            bigint project_id FK
            bigint user_id FK
            string member_role }
    PROJECT_TASK { bigint id PK
            bigint project_id FK
            bigint assignee_id FK
            string status }
    NOTIFICATION { bigint id PK
            bigint user_id FK
            string link_url
            boolean is_read }
    CONTENT_REPORT { bigint id PK
            bigint reporter_id FK
            string target_type
            bigint target_id
            string status }
```

Database rules that protect data:

- `answer_vote`: unique (answer, user) so one student can vote once per answer.
- `study_request.active_pair_key` and `project_join_request.active_key`: generated columns with a unique index, so duplicate active requests are impossible even if two clicks arrive at the same time.
- `project_membership`: unique (project, user).
- Accounts are suspended, not deleted. Deleting a question deletes its answers and votes.
