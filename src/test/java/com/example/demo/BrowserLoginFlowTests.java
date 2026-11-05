package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

// Signs in and saves a change exactly as the browser page does, using only
// cookies and headers: no test shortcuts for login or CSRF. The extra
// property gives this class its own application context, because Spring
// Security's test helpers used elsewhere replace the CSRF cookie store.
@SpringBootTest(properties = "test.context=browser-login-flow")
@AutoConfigureMockMvc
class BrowserLoginFlowTests {

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
    }

    @Test
    void signInThenSaveAChangeWithTheCsrfCookie() throws Exception {
        // 1. Open the login page: the server hands out a session and a CSRF cookie.
        MvcResult loginPage = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginPage.getRequest().getSession();
        Cookie loginToken = loginPage.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(loginToken, "The login page should set the XSRF-TOKEN cookie.");

        // 2. Submit the login form with the token.
        MvcResult login = mockMvc.perform(post("/login")
                        .session(session)
                        .cookie(loginToken)
                        .header("X-XSRF-TOKEN", loginToken.getValue())
                        .param("username", "ada")
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        MockHttpSession signedIn = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(signedIn);

        // 3. The page asks who is signed in and receives a fresh token.
        MvcResult me = mockMvc.perform(get("/api/me").session(signedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ada"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();
        Cookie token = me.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(token, "Signed-in pages should receive an XSRF-TOKEN cookie.");

        // 4. A change sent with the token in the header is accepted...
        mockMvc.perform(post("/api/departments")
                        .session(signedIn)
                        .cookie(token)
                        .header("X-XSRF-TOKEN", token.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Research\"}"))
                .andExpect(status().isCreated());

        // 5. ...and the same change without the header is refused.
        mockMvc.perform(post("/api/departments")
                        .session(signedIn)
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Legal\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordStaysOnTheLoginPage() throws Exception {
        MvcResult loginPage = mockMvc.perform(get("/login")).andReturn();
        Cookie token = loginPage.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(token);
        mockMvc.perform(post("/login")
                        .session((MockHttpSession) loginPage.getRequest().getSession())
                        .cookie(token)
                        .header("X-XSRF-TOKEN", token.getValue())
                        .param("username", "ada")
                        .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }
}
