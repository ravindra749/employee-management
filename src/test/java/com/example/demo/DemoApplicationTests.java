package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void clearEmployees() {
        employeeRepository.deleteAll();
    }

    @Test
    void employeeCrudWorkflowPersistsAndReturnsExpectedResponses()
            throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        MvcResult createResult = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":99,"firstName":"Ada","lastName":"Lovelace",
                                 "email":"ada@example.com","department":"Research"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andReturn();
        String createdJson = createResult.getResponse().getContentAsString();
        long id = Long.parseLong(
                createdJson.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/employees/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada@example.com"));

        mockMvc.perform(put("/api/employees/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ada","lastName":"Byron",
                                 "email":"ada.byron@example.com","department":"Engineering"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Byron"))
                .andExpect(jsonPath("$.department").value("Engineering"));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(delete("/api/employees/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/employees/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Employee not found with id " + id));
    }

    @Test
    void invalidEmployeeIsRejectedWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Lovelace",
                                 "email":"not-an-email","department":"Research"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void deletingUnknownEmployeeReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/employees/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Employee not found with id 999999"));
    }

    @Test
    void frontendPageAndAssetsAreServedBySpringBoot() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Employee management")));

        mockMvc.perform(get("/script.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("/api/employees")));

        mockMvc.perform(get("/styles.css"))
                .andExpect(status().isOk());
    }

    @Test
    void frontendExposesAccessibleCrudControlsAndSafeApiWiring()
            throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("<form id=\"employeeForm\">")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("id=\"email\" name=\"email\"")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("type=\"email\"")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("id=\"cancelEdit\"")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("aria-live=\"polite\"")));

        MvcResult scriptResult = mockMvc.perform(get("/script.js"))
                .andExpect(status().isOk())
                .andReturn();
        String script = scriptResult.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(script.contains("method: isEditing ? \"PUT\" : \"POST\""));
        org.junit.jupiter.api.Assertions.assertTrue(script.contains("method: \"DELETE\""));
        org.junit.jupiter.api.Assertions.assertTrue(script.contains("button.dataset.action === \"edit\""));
        org.junit.jupiter.api.Assertions.assertTrue(script.contains("window.confirm("));
        org.junit.jupiter.api.Assertions.assertTrue(script.contains("cell.textContent = value ?? \"\";"));
        org.junit.jupiter.api.Assertions.assertFalse(script.contains(".innerHTML"));
    }
}
