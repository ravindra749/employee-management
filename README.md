# Employee Management

A Spring Boot 4 and MySQL web application for managing employees and their
departments. One page at `http://localhost:8080/` talks to a REST API under
`/api`. Users sign in; admins can change data, viewers can only read.

## Features

- Employees: add, view, edit and delete. Each employee belongs to one department.
- Departments: add, rename and delete. A department that still has employees
  cannot be deleted (`409`).
- Employee list: paged (10, 20 or 50 rows), sortable by any column, and
  filterable by department.
- Validation: every field is required, emails must be well formed, text is
  limited to 255 characters. Invalid input returns `400` with one error per field.
- Unique emails (any letter case) and unique department names (`409`), backed
  by database constraints.
- Sign-in with two roles: `ADMIN` (read and change) and `VIEWER` (read only).
- API documentation with Swagger UI at `/swagger-ui.html` (sign-in required).

## Run with Docker (recommended)

Needs Docker Desktop. This starts MySQL 8.4 and the app together.

```powershell
Copy-Item .env.example .env   # then fill in every empty value in .env
docker compose up --build
```

Open `http://localhost:8080/` and sign in with `APP_ADMIN_USERNAME` and
`APP_ADMIN_PASSWORD` from `.env`. Stop with `Ctrl+C`; `docker compose down`
removes the containers and keeps the data; `docker compose down -v` also
deletes the database volume. Never commit `.env`; it is in `.gitignore`.

## Run locally without Docker

Use Java 21 and a running MySQL instance. Create the `employee_management`
database yourself, then set these environment variables in the shell or IDE
run configuration:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/employee_management"
$env:DB_USERNAME = "<your MySQL username>"
$env:DB_PASSWORD = "<your MySQL password>"
$env:APP_ADMIN_USERNAME = "admin"
$env:APP_ADMIN_PASSWORD = "<at least 12 characters>"
.\mvnw.cmd spring-boot:run
```

`APP_VIEWER_USERNAME` and `APP_VIEWER_PASSWORD` optionally add a read-only
account. These accounts are created on startup only if they do not exist yet;
changing the variables later does not change an existing account's password.

## API

All routes need a signed-in user. `GET` requests are open to both roles;
`POST`, `PUT` and `DELETE` need `ADMIN` and a CSRF token (see Security).

| Method | Path | Result |
|---|---|---|
| `GET` | `/api/employees?page=0&size=20&sort=lastName&direction=asc&departmentId=3` | One page of employees; every parameter is optional |
| `GET` | `/api/employees/{id}` | One employee, or `404` |
| `POST` | `/api/employees` | Creates: `201`, `400` or `409` |
| `PUT` | `/api/employees/{id}` | Updates: `200`, `400`, `404` or `409` |
| `DELETE` | `/api/employees/{id}` | Deletes: `204` or `404` |
| `GET` | `/api/departments` | All departments, sorted by name |
| `GET` | `/api/departments/{id}` | One department, or `404` |
| `POST` | `/api/departments` | Creates: `201`, `400` or `409` |
| `PUT` | `/api/departments/{id}` | Renames: `200`, `400`, `404` or `409` |
| `DELETE` | `/api/departments/{id}` | Deletes: `204`, `404`, or `409` if it has employees |
| `GET` | `/api/me` | The signed-in user's name and role |

Employee request body: `{"firstName", "lastName", "email", "departmentId"}`.
Employee responses add `id` and `departmentName`. Paging: `size` is 1 to 100;
`sort` is one of `id`, `firstName`, `lastName`, `email`, `departmentName`;
`direction` is `asc` or `desc`. A list response looks like
`{"content": [...], "page": 0, "size": 20, "totalElements": 45, "totalPages": 3}`.

## Security

- Spring Security form login at `/login`, session cookie, sign out from the page.
- Passwords are stored only as BCrypt hashes in the `app_user` table.
- API calls without a session get `401`; a viewer changing data gets `403`.
- CSRF protection is on. The server sets an `XSRF-TOKEN` cookie; the page sends
  it back in the `X-XSRF-TOKEN` header with every change.
- No credentials are in the code: database and first-account settings come from
  environment variables.

## Database

Flyway applies the versioned migrations in `src/main/resources/db/migration`
on startup; Hibernate only validates the schema.

| Migration | Change |
|---|---|
| `V1__create_employee_table.sql` | `employee` table |
| `V2__unique_employee_email.sql` | Unique email constraint |
| `V3__create_department_table.sql` | `department` table; existing employees are linked to it |
| `V4__create_app_user_table.sql` | `app_user` table for sign-in |

### Upgrading an existing database

V2 fails if two employees already share an email. Check first:

```sql
SELECT LOWER(email), COUNT(*) FROM employee GROUP BY LOWER(email) HAVING COUNT(*) > 1;
```

V3 turns each distinct department name (ignoring letter case and surrounding
spaces) into one `department` row and links every employee to it, then drops
the old text column. Take a backup before upgrading a database that holds real
data.

## Tests

```powershell
.\mvnw.cmd test
```

Most tests use the in-memory H2 database in MySQL compatibility mode and need no
credentials. `MySqlEmployeeIntegrationTests` starts a temporary MySQL 8.4
container with Testcontainers when Docker is running (it is skipped otherwise)
and checks the migrations, including V3 on existing data, against real MySQL.
It never uses `DB_URL` or a developer database.

GitHub Actions (`.github/workflows/ci.yml`) runs every test, MySQL included, and
builds the Docker image on every push and pull request.
