package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class DepartmentTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @BeforeEach
    void resetData() {
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    @Test
    void departmentsAreCreatedListedByNameAndRenamed() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  Research  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Research"));
        Department engineering = departmentRepository.save(new Department("Engineering"));

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Engineering"))
                .andExpect(jsonPath("$[1].name").value("Research"));

        mockMvc.perform(put("/api/departments/{id}", engineering.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Platform Engineering\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Platform Engineering"));

        mockMvc.perform(get("/api/departments/{id}", engineering.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Platform Engineering"));
    }

    @Test
    void duplicateAndBlankNamesAreRejected() throws Exception {
        departmentRepository.save(new Department("Research"));

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"RESEARCH\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.name").exists());

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void departmentWithEmployeesCannotBeDeleted() throws Exception {
        Department research = departmentRepository.save(new Department("Research"));
        Department legal = departmentRepository.save(new Department("Legal"));
        employeeRepository.save(new Employee("Ada", "Lovelace", "ada@example.com", research));

        mockMvc.perform(delete("/api/departments/{id}", research.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "The Research department still has employees. Move or delete them first."));

        mockMvc.perform(delete("/api/departments/{id}", legal.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/departments/{id}", legal.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Department not found with id " + legal.getId()));
    }

    @Test
    void pageOffersADepartmentPanelWiredToTheApi() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("<form id=\"departmentForm\"")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("id=\"departmentList\"")));

        mockMvc.perform(get("/script.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "`/api/departments/${encodeURIComponent(department.id)}`")));
    }
}
