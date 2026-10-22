package com.studysync.service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class WorkflowService {
  private final JdbcTemplate db;

  public WorkflowService(JdbcTemplate db) {
    this.db = db;
  }

  public List<Map<String, Object>> rows(String sql, Object... args) {
    return db.queryForList(sql, args);
  }

  public Map<String, Object> one(String sql, Object... args) {
    var list = rows(sql, args);
    if (list.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return list.getFirst();
  }

  private long insert(String sql, Object... args) {
    var key = new GeneratedKeyHolder();
    db.update(
        c -> {
          PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
          return s;
        },
        key);
    return Objects.requireNonNull(key.getKey()).longValue();
  }

  private void require(boolean allowed) {
    if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }

  private void valid(boolean ok, String message) {
    if (!ok) throw new IllegalArgumentException(message);
  }

  private boolean eq(Object id, long user) {
    return ((Number) id).longValue() == user;
  }

  public String text(Map<String, String> f, String key, int max) {
    String value = f.getOrDefault(key, "").trim();
    valid(
        !value.isBlank() && value.length() <= max,
        "Please enter " + key + " (maximum " + max + " characters).");
    return value;
  }

  private long subject(String name) {
    String key = name.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    db.update(
        "INSERT INTO subject(name,name_key) VALUES (?,?) ON DUPLICATE KEY UPDATE name_key=name_key",
        name,
        key);
    return ((Number) one("SELECT id FROM subject WHERE name_key=?", key).get("id")).longValue();
  }

  public List<Map<String, Object>> questions(String subject, String topic) {
    return rows(
        "SELECT q.*,s.name AS subject_name,p.full_name FROM question q JOIN subject s ON"
            + " s.id=q.subject_id JOIN student_profile p ON p.user_id=q.author_id WHERE (?='' OR"
            + " s.name LIKE ?) AND (?='' OR q.topic LIKE ?) ORDER BY q.created_at DESC LIMIT 200",
        subject,
        "%" + subject + "%",
        topic,
        "%" + topic + "%");
  }

  public Map<String, Object> question(long id) {
    return one(
        "SELECT q.*,s.name AS subject_name,p.full_name FROM question q JOIN subject s ON"
            + " s.id=q.subject_id JOIN student_profile p ON p.user_id=q.author_id WHERE q.id=?",
        id);
  }

  public List<Map<String, Object>> answers(long id, long user) {
    return rows(
        "SELECT a.*,p.full_name,(SELECT COUNT(*) FROM answer_vote v WHERE v.answer_id=a.id) AS"
            + " helpful,(SELECT COUNT(*) FROM answer_vote v WHERE v.answer_id=a.id AND v.user_id=?)"
            + " AS voted FROM answer a JOIN student_profile p ON p.user_id=a.author_id WHERE"
            + " a.question_id=? ORDER BY a.created_at",
        user,
        id);
  }

  public long ask(long user, Map<String, String> f) {
    return insert(
        "INSERT INTO question(author_id,subject_id,title,topic,description) VALUES (?,?,?,?,?)",
        user,
        subject(text(f, "subject", 100)),
        text(f, "title", 200),
        text(f, "topic", 100),
        text(f, "description", 10000));
  }

  public void answer(long user, long id, Map<String, String> f) {
    question(id);
    insert(
        "INSERT INTO answer(question_id,author_id,body) VALUES (?,?,?)",
        id,
        user,
        text(f, "body", 10000));
    db.update(
        "UPDATE question SET status=CASE WHEN status='RESOLVED' THEN status ELSE 'ANSWERED'"
            + " END,updated_at=CURRENT_TIMESTAMP WHERE id=?",
        id);
  }

  public void helpful(long user, long question, long answer) {
    var a = one("SELECT * FROM answer WHERE id=? AND question_id=?", answer, question);
    valid(!eq(a.get("author_id"), user), "You cannot mark your own answer helpful.");
    db.update(
        "INSERT INTO answer_vote(answer_id,user_id) VALUES (?,?) ON DUPLICATE KEY UPDATE"
            + " user_id=user_id",
        answer,
        user);
  }

  public void resolve(long user, long id) {
    require(eq(question(id).get("author_id"), user));
    valid(
        !rows("SELECT id FROM answer WHERE question_id=?", id).isEmpty(),
        "A question needs an answer before it can be resolved.");
    db.update("UPDATE question SET status='RESOLVED',updated_at=CURRENT_TIMESTAMP WHERE id=?", id);
  }

  public List<Map<String, Object>> partners(long user, String search) {
    return rows(
        "SELECT p.*,s.name AS subject_name FROM student_profile p JOIN users u ON u.id=p.user_id"
            + " JOIN student_subject_preference pref ON pref.user_id=p.user_id AND"
            + " pref.preference_type='CAN_HELP' JOIN subject s ON s.id=pref.subject_id WHERE"
            + " p.discoverable=TRUE AND u.status='ACTIVE' AND p.user_id<>? AND ((?<>'' AND s.name"
            + " LIKE ?) OR (?='' AND EXISTS (SELECT 1 FROM student_subject_preference mine WHERE"
            + " mine.user_id=? AND mine.subject_id=s.id AND mine.preference_type='WANT_TO_LEARN')))"
            + " ORDER BY p.full_name LIMIT 200",
        user,
        search,
        "%" + search + "%",
        search,
        user);
  }

  public List<Map<String, Object>> requests(long user) {
    return rows(
        "SELECT r.*,a.full_name AS sender_name,b.full_name AS recipient_name,(SELECT s.name FROM"
            + " study_request_subject rs JOIN subject s ON s.id=rs.subject_id WHERE"
            + " rs.request_id=r.id LIMIT 1) AS subject_name FROM study_request r JOIN"
            + " student_profile a ON a.user_id=r.sender_id JOIN student_profile b ON"
            + " b.user_id=r.recipient_id WHERE r.sender_id=? OR r.recipient_id=? ORDER BY"
            + " r.created_at DESC",
        user,
        user);
  }

  public Map<String, Object> request(long user, long id) {
    var r = one("SELECT * FROM study_request WHERE id=? FOR UPDATE", id);
    require(eq(r.get("sender_id"), user) || eq(r.get("recipient_id"), user));
    return r;
  }

  public void invite(long user, long target, Map<String, String> f) {
    valid(user != target, "Choose another student.");
    String name = text(f, "subject", 100);
    var match =
        one(
            "SELECT pref.subject_id FROM student_subject_preference pref JOIN subject s ON"
                + " s.id=pref.subject_id JOIN student_profile p ON p.user_id=pref.user_id JOIN"
                + " users u ON u.id=pref.user_id WHERE pref.user_id=? AND"
                + " pref.preference_type='CAN_HELP' AND s.name_key=? AND p.discoverable=TRUE AND"
                + " u.status='ACTIVE'",
            target,
            name.toLowerCase(Locale.ROOT).replaceAll("\\s+", " "));
    long id =
        insert(
            "INSERT INTO study_request(sender_id,recipient_id,message) VALUES (?,?,?)",
            user,
            target,
            text(f, "message", 500));
    db.update(
        "INSERT INTO study_request_subject(request_id,subject_id) VALUES (?,?)",
        id,
        match.get("subject_id"));
  }

  public void respond(long user, long id, String decision) {
    var r = request(user, id);
    valid(
        "PENDING".equals(r.get("status"))
            || ("ACCEPTED".equals(r.get("status")) && decision.equals("CANCELLED")),
        "This request has already been handled.");
    if (decision.equals("CANCELLED"))
      require(eq(r.get("sender_id"), user) || eq(r.get("recipient_id"), user));
    else {
      require(eq(r.get("recipient_id"), user));
      valid(Set.of("ACCEPTED", "DECLINED").contains(decision), "Invalid decision.");
    }
    db.update(
        "UPDATE study_request SET status=?,responded_at=CURRENT_TIMESTAMP WHERE id=?",
        decision,
        id);
  }

  public List<Map<String, Object>> sessions(long user) {
    return rows(
        "SELECT s.*,a.full_name AS sender_name,b.full_name AS recipient_name,r.status AS"
            + " request_status FROM study_session s JOIN study_request r ON r.id=s.request_id JOIN"
            + " student_profile a ON a.user_id=r.sender_id JOIN student_profile b ON"
            + " b.user_id=r.recipient_id WHERE r.sender_id=? OR r.recipient_id=? ORDER BY"
            + " s.scheduled_at",
        user,
        user);
  }

  public void schedule(long user, long id, Map<String, String> f) {
    var r = request(user, id);
    valid("ACCEPTED".equals(r.get("status")), "Accept the request before scheduling a session.");
    OffsetDateTime time;
    try {
      time = OffsetDateTime.parse(text(f, "scheduledAt", 40));
    } catch (DateTimeException e) {
      throw new IllegalArgumentException(
          "Enter a future date and time including its UTC offset, for example"
              + " 2026-11-01T18:00+05:30.");
    }
    valid(time.toInstant().isAfter(Instant.now()), "Choose a future session time.");
    String method = text(f, "method", 20);
    valid(Set.of("ONLINE", "IN_PERSON").contains(method), "Choose online or in person.");
    insert(
        "INSERT INTO"
            + " study_session(request_id,proposed_by_id,scheduled_at,study_method,meeting_details)"
            + " VALUES (?,?,?,?,?)",
        id,
        user,
        java.sql.Timestamp.from(time.toInstant()),
        method,
        text(f, "meetingDetails", 500));
  }

  public List<Map<String, Object>> projects(long user, String search) {
    return rows(
        "SELECT p.*,s.full_name FROM project p JOIN student_profile s ON s.user_id=p.owner_id WHERE"
            + " p.title LIKE ? OR p.category LIKE ? OR EXISTS (SELECT 1 FROM project_skill k WHERE"
            + " k.project_id=p.id AND k.skill_name LIKE ?) ORDER BY p.created_at DESC LIMIT 200",
        "%" + search + "%",
        "%" + search + "%",
        "%" + search + "%");
  }

  public Map<String, Object> project(long id) {
    return one(
        "SELECT p.*,s.full_name FROM project p JOIN student_profile s ON s.user_id=p.owner_id WHERE"
            + " p.id=?",
        id);
  }

  public List<Map<String, Object>> members(long id) {
    return rows(
        "SELECT m.*,p.full_name FROM project_membership m JOIN student_profile p ON"
            + " p.user_id=m.user_id WHERE m.project_id=?",
        id);
  }

  public boolean member(long user, long id) {
    return !rows("SELECT id FROM project_membership WHERE project_id=? AND user_id=?", id, user)
        .isEmpty();
  }

  public List<Map<String, Object>> applications(long user, long id) {
    boolean owner = eq(project(id).get("owner_id"), user);
    return rows(
        "SELECT r.*,p.full_name FROM project_join_request r JOIN student_profile p ON"
            + " p.user_id=r.applicant_id WHERE r.project_id=? AND (?=TRUE OR r.applicant_id=?)"
            + " ORDER BY r.created_at",
        id,
        owner,
        user);
  }

  public List<Map<String, Object>> tasks(long user, long id) {
    require(member(user, id));
    return rows(
        "SELECT t.*,p.full_name AS assignee_name FROM project_task t LEFT JOIN student_profile p ON"
            + " p.user_id=t.assignee_id WHERE t.project_id=? ORDER BY t.created_at",
        id);
  }

  public long createProject(long user, Map<String, String> f) {
    int positions;
    try {
      positions = Integer.parseInt(f.getOrDefault("positions", ""));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Choose 1 to 50 open positions.");
    }
    valid(positions >= 1 && positions <= 50, "Choose 1 to 50 open positions.");
    String skills = text(f, "skills", 500);
    var tags = new LinkedHashSet<String>();
    for (String skill : skills.split(",")) {
      valid(
          !skill.isBlank() && skill.trim().length() <= 60,
          "Each skill must contain 1 to 60 characters.");
      tags.add(skill.trim().toLowerCase(Locale.ROOT));
    }
    long id =
        insert(
            "INSERT INTO"
                + " project(owner_id,title,problem_statement,description,category,required_roles,open_positions,expected_duration)"
                + " VALUES (?,?,?,?,?,?,?,?)",
            user,
            text(f, "title", 150),
            text(f, "problem", 1000),
            text(f, "description", 10000),
            text(f, "category", 50),
            text(f, "roles", 500),
            positions,
            text(f, "duration", 100));
    db.update(
        "INSERT INTO project_membership(project_id,user_id,member_role) VALUES (?,?,'OWNER')",
        id,
        user);
    for (String skill : tags)
      db.update("INSERT INTO project_skill(project_id,skill_name) VALUES (?,?)", id, skill);
    return id;
  }

  public void apply(long user, long id, Map<String, String> f) {
    var p = one("SELECT * FROM project WHERE id=? FOR UPDATE", id);
    valid("OPEN".equals(p.get("status")), "This project is closed.");
    valid(!member(user, id), "You already belong to this team.");
    valid(
        p.get("application_deadline") == null
            || !((java.sql.Date) p.get("application_deadline"))
                .toLocalDate()
                .isBefore(LocalDate.now(ZoneOffset.UTC)),
        "The application deadline has passed.");
    insert(
        "INSERT INTO project_join_request(project_id,applicant_id,message) VALUES (?,?,?)",
        id,
        user,
        text(f, "message", 500));
  }

  public void decide(long user, long id, long application, String decision) {
    var p = one("SELECT * FROM project WHERE id=? FOR UPDATE", id);
    var a =
        one(
            "SELECT * FROM project_join_request WHERE id=? AND project_id=? FOR UPDATE",
            application,
            id);
    valid("PENDING".equals(a.get("status")), "This application has already been handled.");
    if ("WITHDRAWN".equals(decision)) require(eq(a.get("applicant_id"), user));
    else {
      require(eq(p.get("owner_id"), user));
      valid(Set.of("ACCEPTED", "REJECTED").contains(decision), "Invalid decision.");
    }
    if ("ACCEPTED".equals(decision)) {
      valid("OPEN".equals(p.get("status")), "Reopen the project before accepting members.");
      int count = members(id).size() - 1;
      valid(count < ((Number) p.get("open_positions")).intValue(), "The team is full.");
      db.update(
          "INSERT INTO project_membership(project_id,user_id,member_role) VALUES (?,?,'MEMBER')",
          id,
          a.get("applicant_id"));
    }
    db.update(
        "UPDATE project_join_request SET status=?,decided_at=CURRENT_TIMESTAMP WHERE id=?",
        decision,
        application);
  }

  public void close(long user, long id, String status) {
    require(eq(project(id).get("owner_id"), user));
    valid(Set.of("OPEN", "CLOSED").contains(status), "Invalid project status.");
    db.update("UPDATE project SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", status, id);
  }

  public void task(long user, long id, Map<String, String> f) {
    require(member(user, id));
    Long assignee = null;
    if (!f.getOrDefault("assignee", "").isBlank()) {
      assignee = Long.valueOf(f.get("assignee"));
      valid(member(assignee, id), "Assign tasks only to team members.");
    }
    LocalDate due = null;
    if (!f.getOrDefault("dueDate", "").isBlank()) {
      try {
        due = LocalDate.parse(f.get("dueDate"));
      } catch (DateTimeException e) {
        throw new IllegalArgumentException("Enter a valid due date.");
      }
    }
    insert(
        "INSERT INTO project_task(project_id,title,description,assignee_id,created_by_id,due_date)"
            + " VALUES (?,?,?,?,?,?)",
        id,
        text(f, "title", 150),
        text(f, "description", 1000),
        assignee,
        user,
        due);
  }

  public void progress(long user, long id, long task, String status) {
    require(member(user, id));
    one("SELECT id FROM project_task WHERE id=? AND project_id=?", task, id);
    valid(
        Set.of("TODO", "IN_PROGRESS", "COMPLETED").contains(status), "Choose a valid task status.");
    db.update(
        "UPDATE project_task SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", status, task);
  }
}
