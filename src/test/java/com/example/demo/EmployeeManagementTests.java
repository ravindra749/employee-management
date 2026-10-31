package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.example.demo.Employee;
import com.example.demo.EmployeeRepository;
import com.example.demo.EmployeeService;

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
public class EmployeeManagementTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void clearEmployees() {
        employeeRepository.deleteAll();
    }

    @Test
    void testGetAllEmployeesWithDepartmentFilter() throws Exception {
        // Test with no department filter
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // Create an employee and add it to the repository
        MvcResult createResult = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
{
                                "id":99,
                                "firstName":"Ada",
                                "lastName":"Lovelace",
                                "email":"ada@example.com",
                                "department":"Research"
                            }
""")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andReturn();
        String createdJson = createResult.getResponse().getContentAsString();
        long id = Long.parseLong(
                createdJson.replaceAll(".*\"id\":(\\d+).*", "$1"));

        // Test with department filter
        mockMvc.perform(get("/api/employees?department=Research"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        // The filter ignores letter case and surrounding spaces
        mockMvc.perform(get("/api/employees?department= research "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        // Test with different department filter
        mockMvc.perform(get("/api/employees?department=Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void overlyLongFieldsAreRejectedWithFieldErrors() throws Exception {
        String longName = "a".repeat(256);
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"" + longName + "\","
                                + "\"lastName\":\"Lovelace\","
                                + "\"email\":\"ada@example.com\","
                                + "\"department\":\"Research\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists());

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void pageOffersADepartmentFilterWiredToTheApi() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("id=\"departmentFilter\"")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("id=\"clearDepartmentFilter\"")));

        mockMvc.perform(get("/script.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/api/employees?department=${encodeURIComponent(department)}")));
    }
}
