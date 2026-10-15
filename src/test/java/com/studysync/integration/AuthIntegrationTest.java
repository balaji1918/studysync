package com.studysync.integration;

import com.studysync.entity.User;
import com.studysync.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Runs the real application against a real, temporary MySQL 8 database (Flyway creates the tables).
 * Needs Docker running. If Docker is not available the test is skipped, not failed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder registerRequest(String email) {
        return post("/register").with(csrf())
                .param("fullName", "Asha Rao")
                .param("email", email)
                .param("college", "Example College")
                .param("department", "AI and Data Science")
                .param("yearOfStudy", "2")
                .param("password", "Secret123")
                .param("confirmPassword", "Secret123");
    }

    @Test
    void registrationSavesUserWithHashedPassword() throws Exception {
        mockMvc.perform(registerRequest("asha1@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        User saved = userRepository.findByEmailIgnoreCase("asha1@example.com").orElseThrow();
        assertTrue(saved.getPasswordHash().startsWith("$2"), "password must be a BCrypt hash");
        assertNotEquals("Secret123", saved.getPasswordHash());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        mockMvc.perform(registerRequest("dup@example.com")).andExpect(status().is3xxRedirection());
        mockMvc.perform(registerRequest("DUP@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
        assertEquals(1, userRepository.findAll().stream().filter(u -> u.getEmail().equals("dup@example.com")).count());
    }

    @Test
    void passwordMismatchIsRejected() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Asha Rao").param("email", "mismatch@example.com")
                        .param("college", "C").param("department", "D").param("yearOfStudy", "2")
                        .param("password", "Secret123").param("confirmPassword", "Different123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
        assertTrue(userRepository.findByEmailIgnoreCase("mismatch@example.com").isEmpty());
    }

    @Test
    void protectedPagesRedirectToLoginWhenLoggedOut() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void registerWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/register").param("email", "x@example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void landingPageIsPublic() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }
}
