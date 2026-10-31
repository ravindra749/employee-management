# Employee Management Demo

This Spring Boot application serves the frontend from `src/main/resources/static` and exposes employee APIs under `/api/employees`.

## Features

- Add, view, edit, and delete employees from the page at `http://localhost:8080/`.
- Filter the employee list by department. Matching ignores letter case and
  surrounding spaces; an empty filter shows everyone.
- Validation: every field is required, emails must be well formed, and text
  fields are limited to 255 characters. Invalid input returns `400` with
  field errors.
- Each email address belongs to one employee. A duplicate, in any letter case,
  returns `409 Conflict`, and the database enforces it with a unique constraint.

### API

| Method | Path | Result |
|---|---|---|
| `GET` | `/api/employees` | All employees |
| `GET` | `/api/employees?department=Research` | Employees in one department |
| `GET` | `/api/employees/{id}` | One employee, or `404` |
| `POST` | `/api/employees` | Creates an employee: `201`, `400`, or `409` |
| `PUT` | `/api/employees/{id}` | Updates an employee: `200`, `400`, `404`, or `409` |
| `DELETE` | `/api/employees/{id}` | Deletes an employee: `204` or `404` |

## Local configuration

Use Java 21 and a running MySQL instance. Create or select the `employee_management` database yourself, then set these environment variables in the shell or IDE run configuration. Keep the password out of source control.

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/employee_management"
$env:DB_USERNAME = "<your MySQL username>"
$env:DB_PASSWORD = "<your MySQL password>"
```

Start the app from this directory:

```powershell
.\mvnw.cmd spring-boot:run
```

The application will not create the MySQL database for you. Flyway applies versioned SQL migrations from `src/main/resources/db/migration`; Hibernate validates the schema and does not alter it.

## Upgrading an existing database

Migration `V2__unique_employee_email.sql` adds a unique constraint on `email`.
If an existing `employee_management` database already has two employees with
the same email, the migration fails and the app does not start. Check first:

```sql
SELECT LOWER(email), COUNT(*) FROM employee GROUP BY LOWER(email) HAVING COUNT(*) > 1;
```

No rows means the upgrade is safe. Otherwise change or remove the duplicates,
then start the app.

## Tests

```powershell
.\mvnw.cmd test
```

The standard application tests use the in-memory H2 database in MySQL
compatibility mode and do not require MySQL credentials.

An additional MySQL integration test is available:

```powershell
.\mvnw.cmd -Dtest=MySqlEmployeeIntegrationTests test
```

When a Docker-compatible container runtime is available, Testcontainers starts
a temporary MySQL 8.4 container with a dedicated `employee_management_test`
database. The test verifies the Flyway migration and employee API against real
MySQL. It never uses `DB_URL`, the configured local MySQL service, or a
developer database. Testcontainers skips this test when Docker is unavailable.
