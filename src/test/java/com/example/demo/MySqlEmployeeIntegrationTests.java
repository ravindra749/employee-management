package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class MySqlEmployeeIntegrationTests {

    @Container
    private static final MySQLContainer mysql = new MySQLContainer(
            DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("employee_management_test")
            .withUsername("integration_test")
            .withPassword("integration_test_only");

    @DynamicPropertySource
    static void configureDisposableDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add(
                "spring.datasource.driver-class-name",
                mysql::getDriverClassName);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearEmployees() {
        jdbcTemplate.update("DELETE FROM employee");
    }

    @Test
    void flywayMigrationAndEmployeeApiWorkAgainstRealMySql() throws Exception {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() "
                        + "AND table_name = 'employee' ORDER BY ordinal_position",
                String.class);
        assertEquals(
                List.of("id", "first_name", "last_name", "email", "department"),
                columns,
                "Flyway should create the expected MySQL table columns.");

        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history "
                        + "WHERE version = '1' AND success = 1",
                Integer.class);
        assertEquals(1, appliedMigrations);

        Integer emailConstraintMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history "
                        + "WHERE version = '2' AND success = 1",
                Integer.class);
        assertEquals(1, emailConstraintMigrations,
                "Flyway should apply the unique email constraint.");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Grace","lastName":"Hopper",
                                 "email":"grace.hopper@example.com","department":"Navy"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("grace.hopper@example.com"));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee WHERE email = ?",
                Integer.class,
                "grace.hopper@example.com"));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Grace"));

        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM employee WHERE email = ?",
                Long.class,
                "grace.hopper@example.com");
        assertNotNull(id);

        mockMvc.perform(get("/api/employees/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Hopper"));

        mockMvc.perform(put("/api/employees/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Grace","lastName":"Murray",
                                 "email":"grace.hopper@example.com","department":"Computing"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Murray"))
                .andExpect(jsonPath("$.department").value("Computing"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee "
                        + "WHERE id = ? AND last_name = ? AND department = ?",
                Integer.class,
                id,
                "Murray",
                "Computing"));

        mockMvc.perform(delete("/api/employees/{id}", id))
                .andExpect(status().isNoContent());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee WHERE id = ?",
                Integer.class,
                id));
        mockMvc.perform(get("/api/employees/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Employee not found with id " + id));
    }

    @Test
    void departmentFilterAndUniqueEmailWorkAgainstRealMySql() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ada","lastName":"Lovelace",
                                 "email":"ada@example.com","department":"Research"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Linus","lastName":"Torvalds",
                                 "email":"linus@example.com","department":"Engineering"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/employees").param("department", " research "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Ada"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Augusta","lastName":"King",
                                 "email":"ADA@EXAMPLE.COM","department":"Research"}
                                """))
                .andExpect(status().isConflict());

        // The V2 constraint also rejects a duplicate written without the API.
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO employee (first_name, last_name, email, department) "
                        + "VALUES ('Augusta', 'King', 'ada@example.com', 'Research')"));
    }
}
