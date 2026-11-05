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
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
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
@Import(SignedInAsAdmin.class)
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

    @Autowired
    private DepartmentRepository departmentRepository;

    private Long researchId;
    private Long engineeringId;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM employee");
        jdbcTemplate.update("DELETE FROM department");
        researchId = departmentRepository.save(new Department("Research")).getId();
        engineeringId = departmentRepository.save(new Department("Engineering")).getId();
    }

    @Test
    void flywayMigrationAndEmployeeApiWorkAgainstRealMySql() throws Exception {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() "
                        + "AND table_name = 'employee' ORDER BY ordinal_position",
                String.class);
        assertEquals(
                List.of("id", "first_name", "last_name", "email", "department_id"),
                columns,
                "Flyway should leave the expected MySQL table columns.");

        for (String version : List.of("1", "2", "3")) {
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history "
                            + "WHERE version = ? AND success = 1",
                    Integer.class, version),
                    "Flyway should apply migration V" + version + ".");
        }

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Grace","lastName":"Hopper",
                                 "email":"grace.hopper@example.com","departmentId":%d}
                                """.formatted(researchId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.departmentName").value("Research"));

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
                                 "email":"grace.hopper@example.com","departmentId":%d}
                                """.formatted(engineeringId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Murray"))
                .andExpect(jsonPath("$.departmentName").value("Engineering"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee "
                        + "WHERE id = ? AND last_name = ? AND department_id = ?",
                Integer.class,
                id,
                "Murray",
                engineeringId));

        mockMvc.perform(delete("/api/employees/{id}", id))
                .andExpect(status().isNoContent());
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
                                 "email":"ada@example.com","departmentId":%d}
                                """.formatted(researchId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Linus","lastName":"Torvalds",
                                 "email":"linus@example.com","departmentId":%d}
                                """.formatted(engineeringId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/employees").param("departmentId", researchId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Ada"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Augusta","lastName":"King",
                                 "email":"ADA@EXAMPLE.COM","departmentId":%d}
                                """.formatted(researchId)))
                .andExpect(status().isConflict());

        // The V2 constraint also rejects a duplicate written without the API.
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO employee (first_name, last_name, email, department_id) "
                        + "VALUES ('Augusta', 'King', 'ada@example.com', ?)", researchId));

        mockMvc.perform(delete("/api/departments/{id}", researchId))
                .andExpect(status().isConflict());
    }

    @Test
    void departmentMigrationKeepsExistingEmployeesOnRealMySql() {
        // A second, empty database in the same container, created as root.
        DriverManagerDataSource root = new DriverManagerDataSource(
                mysql.getJdbcUrl(), "root", mysql.getPassword());
        new JdbcTemplate(root).execute("CREATE DATABASE IF NOT EXISTS v3_migration_check");
        String url = mysql.getJdbcUrl().replace(
                "/employee_management_test", "/v3_migration_check");
        DepartmentMigrationCheck.verify(
                new DriverManagerDataSource(url, "root", mysql.getPassword()));
    }
}
