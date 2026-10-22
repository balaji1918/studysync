package com.studysync.controller;

import com.studysync.security.StudySyncUserDetails;
import com.studysync.service.WorkflowService;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WorkflowController {
  private final WorkflowService service;

  public WorkflowController(WorkflowService service) {
    this.service = service;
  }

  @GetMapping("/questions")
  public String questions(
      @RequestParam(defaultValue = "") String subject,
      @RequestParam(defaultValue = "") String topic,
      Model m) {
    m.addAttribute("questions", service.questions(subject, topic));
    m.addAttribute("subject", subject);
    m.addAttribute("topic", topic);
    return "workflows/questions";
  }

  @GetMapping("/questions/{id}")
  public String question(
      @PathVariable long id, @AuthenticationPrincipal StudySyncUserDetails me, Model m) {
    m.addAttribute("q", service.question(id));
    m.addAttribute("answers", service.answers(id, me.getId()));
    m.addAttribute("me", me.getId());
    return "workflows/question";
  }

  @GetMapping("/partners")
  public String partners(
      @RequestParam(defaultValue = "") String subject,
      @AuthenticationPrincipal StudySyncUserDetails me,
      Model m) {
    m.addAttribute("partners", service.partners(me.getId(), subject));
    m.addAttribute("subject", subject);
    return "workflows/partners";
  }

  @GetMapping("/study-requests")
  public String requests(@AuthenticationPrincipal StudySyncUserDetails me, Model m) {
    m.addAttribute("requests", service.requests(me.getId()));
    m.addAttribute("sessions", service.sessions(me.getId()));
    m.addAttribute("me", me.getId());
    return "workflows/requests";
  }

  @GetMapping("/projects")
  public String projects(
      @RequestParam(defaultValue = "") String search,
      @AuthenticationPrincipal StudySyncUserDetails me,
      Model m) {
    m.addAttribute("projects", service.projects(me.getId(), search));
    m.addAttribute("search", search);
    return "workflows/projects";
  }

  @GetMapping("/projects/{id}")
  public String project(
      @PathVariable long id, @AuthenticationPrincipal StudySyncUserDetails me, Model m) {
    m.addAttribute("p", service.project(id));
    m.addAttribute("members", service.members(id));
    m.addAttribute(
        "skills", service.rows("SELECT skill_name FROM project_skill WHERE project_id=?", id));
    m.addAttribute("applications", service.applications(me.getId(), id));
    boolean member = service.member(me.getId(), id);
    m.addAttribute("member", member);
    m.addAttribute("me", me.getId());
    if (member) m.addAttribute("tasks", service.tasks(me.getId(), id));
    return "workflows/project";
  }

  @PostMapping("/questions")
  public String ask(
      @AuthenticationPrincipal StudySyncUserDetails me,
      @RequestParam Map<String, String> f,
      RedirectAttributes r) {
    try {
      return "redirect:/questions/" + service.ask(me.getId(), f);
    } catch (IllegalArgumentException e) {
      r.addFlashAttribute("error", e.getMessage());
      return "redirect:/questions";
    }
  }

  @PostMapping("/questions/{id}/{action}")
  public String questionAction(
      @PathVariable long id,
      @PathVariable String action,
      @AuthenticationPrincipal StudySyncUserDetails me,
      @RequestParam Map<String, String> f,
      RedirectAttributes r) {
    try {
      switch (action) {
        case "answers" -> service.answer(me.getId(), id, f);
        case "helpful" ->
            service.helpful(me.getId(), id, Long.parseLong(f.getOrDefault("answerId", "0")));
        case "resolve" -> service.resolve(me.getId(), id);
        default ->
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND);
      }
      r.addFlashAttribute("success", "Your change was saved.");
    } catch (IllegalArgumentException e) {
      r.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/questions/" + id;
  }

  @PostMapping("/partners/{id}/request")
  public String invite(
      @PathVariable long id,
      @AuthenticationPrincipal StudySyncUserDetails me,
      @RequestParam Map<String, String> f,
      RedirectAttributes r) {
    try {
      service.invite(me.getId(), id, f);
      r.addFlashAttribute("success", "Study request sent.");
    } catch (IllegalArgumentException e) {
      r.addFlashAttribute("error", e.getMessage());
      return "redirect:/partners";
    } catch (DataIntegrityViolationException e) {
      r.addFlashAttribute("error", "You already have an active request with this student.");
    }
    return "redirect:/study-requests";
  }

  @PostMapping("/study-requests/{id}/{action}")
  public String studyAction(
      @PathVariable long id,
      @PathVariable String action,
      @AuthenticationPrincipal StudySyncUserDetails me,
      @RequestParam Map<String, String> f,
      RedirectAttributes r) {
    try {
      if (action.equals("respond")) service.respond(me.getId(), id, f.getOrDefault("decision", ""));
      else if (action.equals("sessions")) service.schedule(me.getId(), id, f);
      else
        throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.NOT_FOUND);
      r.addFlashAttribute("success", "Study plans updated.");
    } catch (IllegalArgumentException e) {
      r.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/study-requests";
  }

  @PostMapping("/projects")
  public String create(
      @AuthenticationPrincipal StudySyncUserDetails me,
      @RequestParam Map<String, String> f,
      RedirectAttributes r) {
    try {
      return "redirect:/projects/" + service.createProject(me.getId(), f);
    } catch (IllegalArgumentException e) {
      r.addFlashAttribute("error", e.getMessage());
      return "redirect:/projects";
    }
  }

  @PostMapping("/projects/{id}/{action}")
  public String projectAction(
      @PathVariable long id,
      @PathVariable String action,
      @AuthenticationPrincipal StudySyncUserDetails me,
      @RequestParam Map<String, String> f,
      RedirectAttributes r) {
    try {
      switch (action) {
        case "apply" -> service.apply(me.getId(), id, f);
        case "decide" ->
            service.decide(
                me.getId(),
                id,
                Long.parseLong(f.getOrDefault("applicationId", "0")),
                f.getOrDefault("decision", ""));
        case "status" -> service.close(me.getId(), id, f.getOrDefault("status", ""));
        case "tasks" -> service.task(me.getId(), id, f);
        case "progress" ->
            service.progress(
                me.getId(),
                id,
                Long.parseLong(f.getOrDefault("taskId", "0")),
                f.getOrDefault("status", ""));
        default ->
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND);
      }
      r.addFlashAttribute("success", "Project updated.");
    } catch (IllegalArgumentException e) {
      r.addFlashAttribute("error", e.getMessage());
    } catch (DataIntegrityViolationException e) {
      r.addFlashAttribute("error", "An active application or membership already exists.");
    }
    return "redirect:/projects/" + id;
  }
}
