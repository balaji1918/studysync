package com.studysync.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Public pages: landing page, privacy, terms, contact. */
@Controller
public class HomeController {

    private final String contactEmail;

    public HomeController(@Value("${studysync.contact-email:}") String contactEmail) {
        this.contactEmail = contactEmail;
    }

    @GetMapping("/")
    public String home() { return "pages/home"; }

    @GetMapping("/privacy")
    public String privacy() { return "pages/privacy"; }

    @GetMapping("/terms")
    public String terms() { return "pages/terms"; }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("contactEmail", contactEmail.isBlank() ? null : contactEmail);
        return "pages/contact";
    }
}
