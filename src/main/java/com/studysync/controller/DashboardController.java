package com.studysync.controller;

import com.studysync.security.StudySyncUserDetails;
import com.studysync.service.ProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ProfileService profileService;

    public DashboardController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal StudySyncUserDetails me, Model model) {
        model.addAttribute("profile", profileService.viewProfile(me.getId(), me.getId()));
        return "dashboard";
    }
}
