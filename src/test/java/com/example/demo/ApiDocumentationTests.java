package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SignedInAsAdmin.class)
class ApiDocumentationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDescribesEmployeeAndDepartmentRoutes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Employee Management API"))
                .andExpect(jsonPath("$.paths['/api/employees'].get").exists())
                .andExpect(jsonPath("$.paths['/api/employees/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/departments'].post").exists())
                .andExpect(jsonPath("$.components.schemas.EmployeeRequest").exists())
                .andExpect(jsonPath("$.paths['/hello']").doesNotExist());
    }

    @Test
    void swaggerUiPageIsServed() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
