-- StudySync schema V1 (MySQL 8). Flyway runs this once on a clean database.
-- Foreign keys use RESTRICT unless content is meant to disappear with its parent.
-- Accounts are suspended, never deleted, so history stays consistent.

CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    email          VARCHAR(254) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    CONSTRAINT chk_users_role   CHECK (role   IN ('STUDENT', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE student_profile (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    full_name         VARCHAR(100) NOT NULL,
    college           VARCHAR(150) NOT NULL,
    department        VARCHAR(100) NOT NULL,
    year_of_study     INT          NOT NULL,
    introduction      VARCHAR(500) NULL,
    skills            VARCHAR(500) NULL,
    project_interests VARCHAR(500) NULL,
    study_method      VARCHAR(20)  NOT NULL DEFAULT 'EITHER',
    availability      VARCHAR(300) NULL,
    discoverable      BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_profile_user (user_id),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_profile_year   CHECK (year_of_study BETWEEN 1 AND 6),
    CONSTRAINT chk_profile_method CHECK (study_method IN ('ONLINE', 'IN_PERSON', 'EITHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE subject (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    name     VARCHAR(100) NOT NULL,
    name_key VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_subject_key (name_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE student_subject_preference (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         BIGINT      NOT NULL,
    subject_id      BIGINT      NOT NULL,
    preference_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_pref (user_id, subject_id, preference_type),
    KEY idx_pref_subject (subject_id, preference_type),
    CONSTRAINT fk_pref_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_pref_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT chk_pref_type CHECK (preference_type IN ('CAN_HELP', 'WANT_TO_LEARN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE question (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    author_id    BIGINT       NOT NULL,
    subject_id   BIGINT       NOT NULL,
    title        VARCHAR(200) NOT NULL,
    topic        VARCHAR(100) NOT NULL,
    description  TEXT         NOT NULL,
    code_snippet TEXT         NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'UNANSWERED',
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_question_created (created_at),
    KEY idx_question_subject (subject_id, created_at),
    KEY idx_question_status (status, created_at),
    KEY idx_question_author (author_id, created_at),
    CONSTRAINT fk_question_author  FOREIGN KEY (author_id)  REFERENCES users (id),
    CONSTRAINT fk_question_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT chk_question_status CHECK (status IN ('UNANSWERED', 'ANSWERED', 'RESOLVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE answer (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    question_id  BIGINT      NOT NULL,
    author_id    BIGINT      NOT NULL,
    body         TEXT        NOT NULL,
    code_snippet TEXT        NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_answer_question (question_id, created_at),
    KEY idx_answer_author (author_id),
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES question (id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_author   FOREIGN KEY (author_id)   REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE answer_vote (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    answer_id  BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_vote_once (answer_id, user_id),
    CONSTRAINT fk_vote_answer FOREIGN KEY (answer_id) REFERENCES answer (id) ON DELETE CASCADE,
    CONSTRAINT fk_vote_user   FOREIGN KEY (user_id)   REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- active_pair_key: while a request is PENDING or ACCEPTED it holds "smallerId-largerId", so the
-- UNIQUE index blocks a second active request between the same two students in either direction.
-- Otherwise it is NULL (MySQL allows many NULLs in a unique index).
CREATE TABLE study_request (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    sender_id       BIGINT       NOT NULL,
    recipient_id    BIGINT       NOT NULL,
    message         VARCHAR(500) NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    responded_at    DATETIME(6)  NULL,
    active_pair_key VARCHAR(41) GENERATED ALWAYS AS (
        CASE WHEN status IN ('PENDING', 'ACCEPTED')
             THEN CONCAT(LEAST(sender_id, recipient_id), '-', GREATEST(sender_id, recipient_id))
        END) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uq_study_request_active_pair (active_pair_key),
    KEY idx_study_request_recipient (recipient_id, status),
    KEY idx_study_request_sender (sender_id, status),
    CONSTRAINT fk_study_request_sender    FOREIGN KEY (sender_id)    REFERENCES users (id),
    CONSTRAINT fk_study_request_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    CONSTRAINT chk_study_request_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE study_request_subject (
    request_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    PRIMARY KEY (request_id, subject_id),
    CONSTRAINT fk_srs_request FOREIGN KEY (request_id) REFERENCES study_request (id) ON DELETE CASCADE,
    CONSTRAINT fk_srs_subject FOREIGN KEY (subject_id) REFERENCES subject (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE study_session (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    request_id      BIGINT       NOT NULL,
    subject_id      BIGINT       NULL,
    proposed_by_id  BIGINT       NOT NULL,
    scheduled_at    DATETIME(6)  NOT NULL,
    study_method    VARCHAR(20)  NOT NULL,
    meeting_details VARCHAR(500) NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_session_request (request_id, scheduled_at),
    CONSTRAINT fk_session_request  FOREIGN KEY (request_id)     REFERENCES study_request (id),
    CONSTRAINT fk_session_subject  FOREIGN KEY (subject_id)     REFERENCES subject (id),
    CONSTRAINT fk_session_proposer FOREIGN KEY (proposed_by_id) REFERENCES users (id),
    CONSTRAINT chk_session_method CHECK (study_method IN ('ONLINE', 'IN_PERSON'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE project (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    owner_id             BIGINT        NOT NULL,
    title                VARCHAR(150)  NOT NULL,
    problem_statement    VARCHAR(1000) NOT NULL,
    description          TEXT          NOT NULL,
    category             VARCHAR(50)   NOT NULL,
    required_roles       VARCHAR(500)  NULL,
    open_positions       INT           NOT NULL,
    expected_duration    VARCHAR(100)  NOT NULL,
    application_deadline DATE          NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    created_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_project_status (status, created_at),
    KEY idx_project_category (category, status),
    KEY idx_project_owner (owner_id),
    CONSTRAINT fk_project_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT chk_project_positions CHECK (open_positions BETWEEN 1 AND 50),
    CONSTRAINT chk_project_status CHECK (status IN ('OPEN', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE project_skill (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    project_id BIGINT      NOT NULL,
    skill_name VARCHAR(60) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_project_skill (project_id, skill_name),
    KEY idx_project_skill_name (skill_name),
    CONSTRAINT fk_project_skill_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE project_join_request (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    project_id   BIGINT       NOT NULL,
    applicant_id BIGINT       NOT NULL,
    message      VARCHAR(500) NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    decided_at   DATETIME(6)  NULL,
    active_key   VARCHAR(41) GENERATED ALWAYS AS (
        CASE WHEN status IN ('PENDING', 'ACCEPTED') THEN CONCAT(project_id, '-', applicant_id) END) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uq_join_request_active (active_key),
    KEY idx_join_request_project (project_id, status),
    KEY idx_join_request_applicant (applicant_id, status),
    CONSTRAINT fk_join_project   FOREIGN KEY (project_id)   REFERENCES project (id),
    CONSTRAINT fk_join_applicant FOREIGN KEY (applicant_id) REFERENCES users (id),
    CONSTRAINT chk_join_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE project_membership (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    project_id  BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    member_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_membership (project_id, user_id),
    KEY idx_membership_user (user_id),
    CONSTRAINT fk_membership_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_membership_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT chk_membership_role CHECK (member_role IN ('OWNER', 'MEMBER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE project_task (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    project_id    BIGINT        NOT NULL,
    title         VARCHAR(150)  NOT NULL,
    description   VARCHAR(1000) NULL,
    assignee_id   BIGINT        NULL,
    created_by_id BIGINT        NOT NULL,
    due_date      DATE          NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'TODO',
    created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_task_project (project_id, status),
    KEY idx_task_assignee (assignee_id, status),
    CONSTRAINT fk_task_project  FOREIGN KEY (project_id)    REFERENCES project (id),
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id)   REFERENCES users (id),
    CONSTRAINT fk_task_creator  FOREIGN KEY (created_by_id) REFERENCES users (id),
    CONSTRAINT chk_task_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notification (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(40)  NOT NULL,
    message    VARCHAR(300) NOT NULL,
    link_url   VARCHAR(255) NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_notification_user (user_id, is_read, created_at),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE content_report (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    reporter_id BIGINT       NOT NULL,
    target_type VARCHAR(20)  NOT NULL,
    target_id   BIGINT       NOT NULL,
    reason      VARCHAR(500) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reviewed_by BIGINT       NULL,
    reviewed_at DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_report_once (reporter_id, target_type, target_id),
    KEY idx_report_status (status, created_at),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT fk_report_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT chk_report_type   CHECK (target_type IN ('QUESTION', 'ANSWER')),
    CONSTRAINT chk_report_status CHECK (status IN ('OPEN', 'ACTION_TAKEN', 'DISMISSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
