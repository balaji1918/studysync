package com.studysync.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.studysync.repository.UserRepository;
import com.studysync.security.StudySyncUserDetails;
import com.studysync.service.WorkflowService;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    properties = {
      "spring.datasource.url=${WORKFLOW_TEST_DB_URL}",
      "spring.datasource.username=${WORKFLOW_TEST_DB_USERNAME:root}",
      "spring.datasource.password=${WORKFLOW_TEST_DB_PASSWORD:}"
    })
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "WORKFLOW_TEST_DB_URL", matches = ".+")
class WorkflowIntegrationTest {
  @Autowired WorkflowService service;
  @Autowired JdbcTemplate db;
  @Autowired UserRepository users;
  @Autowired MockMvc mvc;
  long alice, bob, eve, subject;

  @BeforeEach
  void students() {
    alice = student("alice");
    bob = student("bob");
    eve = student("eve");
    db.update("INSERT INTO subject(name,name_key) VALUES ('Workflow Maths','workflow maths')");
    subject =
        db.queryForObject("SELECT id FROM subject WHERE name_key='workflow maths'", Long.class);
    db.update(
        "INSERT INTO student_subject_preference(user_id,subject_id,preference_type) VALUES"
            + " (?,?,'WANT_TO_LEARN'),(?,?,'CAN_HELP')",
        alice,
        subject,
        bob,
        subject);
  }

  long student(String name) {
    db.update(
        "INSERT INTO users(email,password_hash) VALUES (?,?)", name + "@workflow.test", "hash");
    long id =
        db.queryForObject(
            "SELECT id FROM users WHERE email=?", Long.class, name + "@workflow.test");
    db.update(
        "INSERT INTO student_profile(user_id,full_name,college,department,year_of_study) VALUES"
            + " (?,?, 'College','CS',2)",
        id,
        name);
    return id;
  }

  StudySyncUserDetails principal(long id) {
    return new StudySyncUserDetails(users.findById(id).orElseThrow());
  }

  long project() {
    return service.createProject(
        alice,
        Map.of(
            "title",
            "Study tool",
            "problem",
            "Help students",
            "description",
            "Build a tool",
            "category",
            "Education",
            "roles",
            "Developer",
            "skills",
            "Java, SQL",
            "positions",
            "1",
            "duration",
            "2 weeks"));
  }

  @Test
  void doubtFlowAndPageRendering() throws Exception {
    long q =
        service.ask(
            alice,
            Map.of(
                "subject",
                "Workflow Maths",
                "topic",
                "Algebra",
                "title",
                "How do equations work?",
                "description",
                "Explain x + 1 = 2"));
    assertEquals("UNANSWERED", service.question(q).get("status"));
    service.answer(bob, q, Map.of("body", "Subtract one from both sides."));
    long a = ((Number) service.answers(q, alice).getFirst().get("id")).longValue();
    service.helpful(alice, q, a);
    service.helpful(alice, q, a);
    assertEquals(1, ((Number) service.answers(q, alice).getFirst().get("helpful")).intValue());
    assertThrows(IllegalArgumentException.class, () -> service.helpful(bob, q, a));
    assertThrows(ResponseStatusException.class, () -> service.resolve(eve, q));
    service.resolve(alice, q);
    assertEquals("RESOLVED", service.question(q).get("status"));
    mvc.perform(get("/questions").with(user(principal(alice))))
        .andExpect(status().isOk())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("How do equations work?")));
    mvc.perform(get("/questions/" + q).with(user(principal(alice))))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Subtract one")));
  }

  @Test
  void studyFlowPrivacyAndStateTransitions() throws Exception {
    assertEquals(1, service.partners(alice, "").size());
    db.update("UPDATE student_profile SET discoverable=FALSE WHERE user_id=?", bob);
    assertTrue(service.partners(alice, "").isEmpty());
    db.update("UPDATE student_profile SET discoverable=TRUE WHERE user_id=?", bob);
    service.invite(
        alice, bob, Map.of("subject", "Workflow Maths", "message", "Please help with algebra"));
    long r = ((Number) service.requests(alice).getFirst().get("id")).longValue();
    assertThrows(ResponseStatusException.class, () -> service.request(eve, r));
    assertThrows(ResponseStatusException.class, () -> service.respond(alice, r, "ACCEPTED"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.schedule(
                alice,
                r,
                Map.of(
                    "scheduledAt",
                    "2099-01-01T10:00+05:30",
                    "method",
                    "ONLINE",
                    "meetingDetails",
                    "Library")));
    service.respond(bob, r, "ACCEPTED");
    service.schedule(
        alice,
        r,
        Map.of(
            "scheduledAt",
            "2099-01-01T10:00+05:30",
            "method",
            "ONLINE",
            "meetingDetails",
            "Agreed meeting link"));
    assertEquals(1, service.sessions(bob).size());
    assertTrue(service.sessions(eve).isEmpty());
    mvc.perform(get("/partners").with(user(principal(alice)))).andExpect(status().isOk());
    mvc.perform(get("/study-requests").with(user(principal(bob))))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Agreed meeting link")));
    service.respond(alice, r, "CANCELLED");
    assertThrows(IllegalArgumentException.class, () -> service.schedule(bob, r, Map.of()));
  }

  @Test
  void projectFlowAuthorizationCapacityAndTaskProgress() throws Exception {
    long p = project();
    service.apply(bob, p, Map.of("message", "I can build Java services"));
    long a = ((Number) service.applications(alice, p).getFirst().get("id")).longValue();
    assertThrows(ResponseStatusException.class, () -> service.decide(eve, p, a, "ACCEPTED"));
    assertThrows(ResponseStatusException.class, () -> service.task(eve, p, Map.of()));
    service.decide(alice, p, a, "ACCEPTED");
    assertTrue(service.member(bob, p));
    service.apply(eve, p, Map.of("message", "Can I help too?"));
    long e = ((Number) service.applications(eve, p).getFirst().get("id")).longValue();
    assertThrows(IllegalArgumentException.class, () -> service.decide(alice, p, e, "ACCEPTED"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.task(
                bob,
                p,
                Map.of(
                    "title", "API", "description", "Build API", "assignee", String.valueOf(eve))));
    service.task(
        bob,
        p,
        Map.of(
            "title",
            "Build API",
            "description",
            "Implement endpoints",
            "assignee",
            String.valueOf(bob),
            "dueDate",
            "2099-01-01"));
    long task = ((Number) service.tasks(alice, p).getFirst().get("id")).longValue();
    assertThrows(ResponseStatusException.class, () -> service.progress(eve, p, task, "COMPLETED"));
    service.progress(bob, p, task, "COMPLETED");
    assertEquals("COMPLETED", service.tasks(alice, p).getFirst().get("status"));
    mvc.perform(get("/projects").with(user(principal(alice)))).andExpect(status().isOk());
    mvc.perform(get("/projects/" + p).with(user(principal(bob))))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Build API")));
    mvc.perform(get("/projects/" + p).with(user(principal(eve))))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Build API"))));
  }

  @Test
  void allWriteRoutesConnectToTheirNextPages() throws Exception {
    var response =
        mvc.perform(
                post("/questions")
                    .with(user(principal(alice)))
                    .with(csrf())
                    .param("subject", "Workflow Maths")
                    .param("topic", "Algebra")
                    .param("title", "Explain equations")
                    .param("description", "I need help"))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    String location = response.getResponse().getRedirectedUrl();
    long q = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    mvc.perform(
            post(location + "/answers")
                .with(user(principal(bob)))
                .with(csrf())
                .param("body", "Here is an explanation"))
        .andExpect(redirectedUrl(location));
    long answer = ((Number) service.answers(q, alice).getFirst().get("id")).longValue();
    mvc.perform(
            post(location + "/helpful")
                .with(user(principal(alice)))
                .with(csrf())
                .param("answerId", String.valueOf(answer)))
        .andExpect(redirectedUrl(location));
    mvc.perform(post(location + "/resolve").with(user(principal(alice))).with(csrf()))
        .andExpect(redirectedUrl(location));
    mvc.perform(
            post("/partners/" + bob + "/request")
                .with(user(principal(alice)))
                .with(csrf())
                .param("subject", "Workflow Maths")
                .param("message", "Please help"))
        .andExpect(redirectedUrl("/study-requests"));
    long r = ((Number) service.requests(alice).getFirst().get("id")).longValue();
    mvc.perform(
            post("/study-requests/" + r + "/respond")
                .with(user(principal(bob)))
                .with(csrf())
                .param("decision", "ACCEPTED"))
        .andExpect(redirectedUrl("/study-requests"));
    mvc.perform(
            post("/study-requests/" + r + "/sessions")
                .with(user(principal(alice)))
                .with(csrf())
                .param("scheduledAt", "2099-01-01T10:00+05:30")
                .param("method", "ONLINE")
                .param("meetingDetails", "Library link"))
        .andExpect(redirectedUrl("/study-requests"));
    var created =
        mvc.perform(
                post("/projects")
                    .with(user(principal(alice)))
                    .with(csrf())
                    .param("title", "Web project")
                    .param("problem", "Student coordination")
                    .param("description", "Build a website")
                    .param("category", "Web")
                    .param("skills", "Java")
                    .param("roles", "Developer")
                    .param("positions", "2")
                    .param("duration", "One month"))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    String url = created.getResponse().getRedirectedUrl();
    long p = Long.parseLong(url.substring(url.lastIndexOf('/') + 1));
    mvc.perform(
            post(url + "/apply")
                .with(user(principal(bob)))
                .with(csrf())
                .param("message", "I know Java"))
        .andExpect(redirectedUrl(url));
    long a = ((Number) service.applications(alice, p).getFirst().get("id")).longValue();
    mvc.perform(
            post(url + "/decide")
                .with(user(principal(alice)))
                .with(csrf())
                .param("applicationId", String.valueOf(a))
                .param("decision", "ACCEPTED"))
        .andExpect(redirectedUrl(url));
    mvc.perform(
            post(url + "/tasks")
                .with(user(principal(bob)))
                .with(csrf())
                .param("title", "Build website")
                .param("description", "Implement homepage"))
        .andExpect(redirectedUrl(url));
    long t = ((Number) service.tasks(bob, p).getFirst().get("id")).longValue();
    mvc.perform(
            post(url + "/progress")
                .with(user(principal(bob)))
                .with(csrf())
                .param("taskId", String.valueOf(t))
                .param("status", "COMPLETED"))
        .andExpect(redirectedUrl(url));
    mvc.perform(
            post(url + "/status")
                .with(user(principal(alice)))
                .with(csrf())
                .param("status", "CLOSED"))
        .andExpect(redirectedUrl(url));
    assertEquals("COMPLETED", service.tasks(bob, p).getFirst().get("status"));
    assertEquals("CLOSED", service.project(p).get("status"));
    assertEquals("RESOLVED", service.question(q).get("status"));
    assertEquals(1, service.sessions(bob).size());
  }

  @Test
  void duplicateRequestsAndApplicationsAreHandled() throws Exception {
    service.invite(alice, bob, Map.of("subject", "Workflow Maths", "message", "Study together"));
    mvc.perform(
            post("/partners/" + bob + "/request")
                .with(user(principal(alice)))
                .with(csrf())
                .param("subject", "Workflow Maths")
                .param("message", "Again"))
        .andExpect(redirectedUrl("/study-requests"))
        .andExpect(flash().attributeExists("error"));
    assertEquals(1, service.requests(alice).size());
    long p = project();
    service.apply(bob, p, Map.of("message", "I can help"));
    mvc.perform(
            post("/projects/" + p + "/apply")
                .with(user(principal(bob)))
                .with(csrf())
                .param("message", "Again"))
        .andExpect(redirectedUrl("/projects/" + p))
        .andExpect(flash().attributeExists("error"));
    assertEquals(1, service.applications(alice, p).size());
  }

  @Test
  void navigationLoginAndCsrf() throws Exception {
    for (String path : new String[] {"/questions", "/partners", "/projects", "/study-requests"})
      mvc.perform(get(path)).andExpect(status().is3xxRedirection());
    mvc.perform(post("/questions").with(user(principal(alice)))).andExpect(status().isForbidden());
    mvc.perform(post("/questions").with(user(principal(alice))).with(csrf()).param("title", " "))
        .andExpect(redirectedUrl("/questions"))
        .andExpect(flash().attributeExists("error"));
    mvc.perform(get("/dashboard").with(user(principal(alice)))).andExpect(status().isOk());
  }
}
