package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

// Shared check that migration V3 keeps existing data: run V1 and V2 on an
// empty database, add employees the old way, then apply the rest.
final class DepartmentMigrationCheck {

    private DepartmentMigrationCheck() {
    }

    static void verify(DataSource dataSource) {
        Flyway.configure().dataSource(dataSource).target("2").load().migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String insert = "INSERT INTO employee (first_name, last_name, email, department) "
                + "VALUES (?, ?, ?, ?)";
        jdbc.update(insert, "Ada", "Lovelace", "ada@example.com", "Research");
        jdbc.update(insert, "Alan", "Turing", "alan@example.com", " research ");
        jdbc.update(insert, "Grace", "Hopper", "grace@example.com", "Navy");

        Flyway.configure().dataSource(dataSource).load().migrate();

        List<String> departments = jdbc.queryForList(
                "SELECT name FROM department ORDER BY name", String.class);
        assertEquals(List.of("Navy", "Research"), departments,
                "Each department name, ignoring case and spaces, becomes one row.");

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT e.email, d.name FROM employee e "
                        + "JOIN department d ON d.id = e.department_id ORDER BY e.email");
        assertEquals(3, rows.size(), "Every employee keeps a department.");
        assertEquals("Research", rows.get(0).get("name"));
        assertEquals("Research", rows.get(1).get("name"));
        assertEquals("Navy", rows.get(2).get("name"));

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO employee (first_name, last_name, email, department_id) "
                        + "VALUES ('Nobody', 'Here', 'nobody@example.com', 987654)"),
                "The foreign key rejects an unknown department.");
    }
}
