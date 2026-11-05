package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SignedInAsAdmin.class)
class PagingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Department research;

    // 25 employees: 01..25, odd numbers in Research, even in Engineering.
    @BeforeEach
    void createEmployees() {
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        research = departmentRepository.save(new Department("Research"));
        Department engineering = departmentRepository.save(new Department("Engineering"));
        for (int i = 1; i <= 25; i++) {
            String number = String.format("%02d", i);
            employeeRepository.save(new Employee(
                    "First" + number, "Last" + number, "person" + number + "@example.com",
                    i % 2 == 1 ? research : engineering));
        }
    }

    @Test
    void employeesComeBackOnePageAtATime() throws Exception {
        mockMvc.perform(get("/api/employees").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].lastName").value("Last01"));

        mockMvc.perform(get("/api/employees").param("size", "10").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[4].lastName").value("Last25"));

        mockMvc.perform(get("/api/employees"))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content.length()").value(20));
    }

    @Test
    void employeesCanBeSortedAndFilteredTogether() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .param("sort", "firstName").param("direction", "desc").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("First25"))
                .andExpect(jsonPath("$.content[2].firstName").value("First23"));

        mockMvc.perform(get("/api/employees").param("sort", "departmentName").param("size", "1"))
                .andExpect(jsonPath("$.content[0].departmentName").value("Engineering"));

        mockMvc.perform(get("/api/employees")
                        .param("departmentId", research.getId().toString())
                        .param("size", "5"))
                .andExpect(jsonPath("$.totalElements").value(13))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].departmentName").value("Research"));
    }

    @Test
    void invalidPagingValuesAreRejected() throws Exception {
        mockMvc.perform(get("/api/employees").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.size").value("must be between 1 and 100"));
        mockMvc.perform(get("/api/employees").param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/employees").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.page").exists());
        mockMvc.perform(get("/api/employees").param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sort").exists());
        mockMvc.perform(get("/api/employees").param("direction", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.direction").value("must be asc or desc"));
    }

    @Test
    void pageOffersSortingAndPagerControls() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-sort=\"departmentName\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "id=\"previousPage\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "id=\"nextPage\"")));

        mockMvc.perform(get("/script.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "apiFetch(`/api/employees?${params}`)")));
    }
}
