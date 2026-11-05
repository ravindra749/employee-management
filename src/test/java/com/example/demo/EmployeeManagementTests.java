package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SignedInAsAdmin.class)
public class EmployeeManagementTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Department research;
    private Department engineering;

    @BeforeEach
    void resetData() {
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        research = departmentRepository.save(new Department("Research"));
        engineering = departmentRepository.save(new Department("Engineering"));
    }

    @Test
    void testGetAllEmployeesWithDepartmentFilter() throws Exception {
        employeeRepository.save(new Employee("Ada", "Lovelace", "ada@example.com", research));
        employeeRepository.save(new Employee("Linus", "Torvalds", "linus@example.com", engineering));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/employees").param("departmentId", research.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Ada"))
                .andExpect(jsonPath("$.content[0].departmentName").value("Research"));

        Department empty = departmentRepository.save(new Department("Legal"));
        mockMvc.perform(get("/api/employees").param("departmentId", empty.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void overlyLongFieldsAreRejectedWithFieldErrors() throws Exception {
        String longName = "a".repeat(256);
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"" + longName + "\","
                                + "\"lastName\":\"Lovelace\","
                                + "\"email\":\"ada@example.com\","
                                + "\"departmentId\":" + research.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists());

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void unknownDepartmentIsRejectedWithAFieldError() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ada","lastName":"Lovelace",
                                 "email":"ada@example.com","departmentId":987654}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.departmentId")
                        .value("does not match an existing department"));
    }

    @Test
    void pageOffersADepartmentFilterWiredToTheApi() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("<select id=\"departmentFilter\"")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("<select id=\"departmentId\"")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("id=\"clearDepartmentFilter\"")));

        mockMvc.perform(get("/script.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "params.set(\"departmentId\", departmentId)")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "apiFetch(\"/api/departments\")")));
    }
}
