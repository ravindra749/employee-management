package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class EmployeeEmailUniquenessTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Department research;

    private Employee ada;
    private Employee grace;

    @BeforeEach
    void createEmployees() {
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        research = departmentRepository.save(new Department("Research"));
        Department navy = departmentRepository.save(new Department("Navy"));
        ada = employeeRepository.save(
                new Employee("Ada", "Lovelace", "ada@example.com", research));
        grace = employeeRepository.save(
                new Employee("Grace", "Hopper", "grace@example.com", navy));
    }

    @Test
    void creatingAnEmployeeWithAnExistingEmailIsRejected() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Augusta","lastName":"King",
                                 "email":"ADA@example.com","departmentId":%d}
                                """.formatted(research.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "An employee with email ADA@example.com already exists."))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void changingAnEmailToAnotherEmployeesEmailIsRejected() throws Exception {
        mockMvc.perform(put("/api/employees/{id}", grace.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Grace","lastName":"Hopper",
                                 "email":"ada@example.com","departmentId":%d}
                                """.formatted(grace.getDepartment().getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void keepingTheSameEmailOnUpdateIsAllowed() throws Exception {
        mockMvc.perform(put("/api/employees/{id}", ada.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ada","lastName":"Byron",
                                 "email":"ada@example.com","departmentId":%d}
                                """.formatted(research.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Byron"));
    }
}
