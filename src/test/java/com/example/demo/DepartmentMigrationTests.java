package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

// Runs the V3 data check on a separate in-memory H2 database.
class DepartmentMigrationTests {

    @Test
    void existingEmployeesAreLinkedToNewDepartmentRows() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:v3-migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        DepartmentMigrationCheck.verify(dataSource);
    }
}
