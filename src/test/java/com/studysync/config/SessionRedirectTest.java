package com.studysync.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionRedirectTest.LoginController.class)
@Import({SecurityConfig.class, SessionRedirectTest.LoginController.class})
class SessionRedirectTest {
    @Autowired MockMvc mvc;

    @RestController
    static class LoginController {
        @GetMapping("/login") String login() { return "login"; }
    }

    @Test
    void staleCookieIsReplacedBeforeRedirectingToLogin() throws Exception {
        var result = mvc.perform(get("/login").with(request -> {
                    request.setRequestedSessionId("stale-cookie");
                    request.setRequestedSessionIdValid(false);
                    return request;
                }))
                .andExpect(redirectedUrl("/login?expired"))
                .andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session, "Redirect must replace the stale session");
        mvc.perform(get("/login?expired").session(session))
                .andExpect(status().isOk()).andExpect(content().string("login"));
    }
}
