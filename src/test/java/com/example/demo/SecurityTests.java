package com.example.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTests {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetData() {
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        appUserRepository.deleteAll();
        appUserRepository.save(new AppUser("ada", passwordEncoder.encode(PASSWORD), AppUser.ADMIN));
        appUserRepository.save(new AppUser("val", passwordEncoder.encode(PASSWORD), AppUser.VIEWER));
    }

    @Test
    void anonymousVisitorsMustSignIn() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/index.html").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void storedAccountsCanSignInWithTheRightPasswordOnly() throws Exception {
        mockMvc.perform(formLogin().user("ada").password(PASSWORD))
                .andExpect(authenticated().withUsername("ada").withRoles(AppUser.ADMIN));
        mockMvc.perform(formLogin().user("val").password(PASSWORD))
                .andExpect(authenticated().withRoles(AppUser.VIEWER));
        mockMvc.perform(formLogin().user("ada").password("wrong-password"))
                .andExpect(unauthenticated());
    }

    @Test
    void viewersCanReadButNotChangeData() throws Exception {
        mockMvc.perform(get("/api/employees").with(user("val").roles(AppUser.VIEWER)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/me").with(user("val").roles(AppUser.VIEWER)))
                .andExpect(jsonPath("$.username").value("val"))
                .andExpect(jsonPath("$.role").value("VIEWER"));
        mockMvc.perform(post("/api/departments")
                        .with(user("val").roles(AppUser.VIEWER)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Research\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void changesWithoutACsrfTokenAreRefused() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .with(user("ada").roles(AppUser.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Research\"}"))
                .andExpect(status().isForbidden());
    }
}
