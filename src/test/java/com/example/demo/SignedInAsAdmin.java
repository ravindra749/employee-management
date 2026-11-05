package com.example.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

// Import this into a test class to send every MockMvc request as a signed-in
// admin with a valid CSRF token. Security itself is tested in SecurityTests.
@TestConfiguration
public class SignedInAsAdmin {

    @Bean
    MockMvcBuilderCustomizer signedInAsAdmin() {
        return builder -> builder.defaultRequest(get("/")
                .with(user("test-admin").roles(AppUser.ADMIN))
                .with(csrf()));
    }
}
