package com.studysync.controller;

import com.studysync.dto.ProfileForm;
import com.studysync.entity.StudyMethod;
import com.studysync.exception.InvalidProfileInputException;
import com.studysync.security.StudySyncUserDetails;
import com.studysync.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /** Edit my own profile. */
    @GetMapping("/profile")
    public String editForm(@AuthenticationPrincipal StudySyncUserDetails me, Model model) {
        model.addAttribute("profileForm", profileService.loadForm(me.getId()));
        model.addAttribute("studyMethods", StudyMethod.values());
        return "profile/edit";
    }

    @PostMapping("/profile")
    public String save(@AuthenticationPrincipal StudySyncUserDetails me,
                       @Valid @ModelAttribute("profileForm") ProfileForm form,
                       BindingResult result,
                       Model model,
                       RedirectAttributes redirect) {
        model.addAttribute("studyMethods", StudyMethod.values());
        if (result.hasErrors()) {
            return "profile/edit";
        }
        try {
            profileService.updateProfile(me.getId(), form);   // id comes from the session, not the form
        } catch (InvalidProfileInputException e) {
            result.rejectValue(e.getField(), "invalid", e.getMessage());
            return "profile/edit";
        }
        redirect.addFlashAttribute("success", "Your profile was saved.");
        return "redirect:/profile";
    }

    /** View a student's profile (mine, or another discoverable student's). */
    @GetMapping("/students/{id}")
    public String view(@PathVariable Long id,
                       @AuthenticationPrincipal StudySyncUserDetails me,
                       Model model) {
        model.addAttribute("profile", profileService.viewProfile(id, me.getId()));
        return "profile/view";
    }
}
