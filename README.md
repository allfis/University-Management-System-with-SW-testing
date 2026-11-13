# University Management System

Spring Boot 3 (Java 17) REST API with Spring Security (session login, BCrypt, RBAC) and PostgreSQL.

## Run with Docker (recommended)

```bash
docker compose up --build
```

API: `http://localhost:8080`. A default admin is created on first start:
`admin` / `admin123` (change via `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `ADMIN_EMAIL`, or a `.env` file).

## Run locally

Needs Java 17+, Maven and a PostgreSQL database named `university` (see `application.yml` for defaults).

```bash
mvn spring-boot:run
mvn test          # integration tests use in-memory H2, no Postgres needed
```

## Access rules (RBAC)

| Resource          | ADMIN | TEACHER | STUDENT |
|-------------------|:-----:|:-------:|:-------:|
| `/api/admins/**`  |  yes  |   no    |   no    |
| `/api/teachers/**`|  yes  |   yes   |   no    |
| `/api/students/**`|  yes  |   no    |   yes   |

Not logged in -> `401`. Logged in but wrong role -> `403`.

## Endpoints

| Method | URL                      | Success | Notes                                  |
|--------|--------------------------|---------|----------------------------------------|
| POST   | `/api/auth/login`        | 200     | JSON `{username, password}`, sets cookie |
| GET    | `/api/auth/me`           | 200     | current user                           |
| POST   | `/api/auth/logout`       | 200     | invalidates the session                |
| GET    | `/api/{resource}`        | 200     | list                                   |
| GET    | `/api/{resource}/{id}`   | 200     | 404 if not found                       |
| POST   | `/api/{resource}`        | 201     | `Location` header; 400 invalid, 409 duplicate |
| PUT    | `/api/{resource}/{id}`   | 200     | password optional (omit to keep)       |
| DELETE | `/api/{resource}/{id}`   | 204     | last admin cannot be deleted (409)     |

`{resource}` = `admins`, `teachers` or `students`.

Request body (extra fields are optional and role-specific: `designation` for teachers, `studentIdNo` + `semester` for students):

```json
{
  "username": "rahim",
  "password": "secret123",
  "fullName": "Rahim Uddin",
  "email": "rahim@kuet.ac.bd",
  "department": "CSE",
  "studentIdNo": "2207117",
  "semester": 6
}
```

## Quick try with curl

```bash
# login (cookie saved in cookies.txt)
curl -c cookies.txt -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}' http://localhost:8080/api/auth/login

# create a student
curl -b cookies.txt -H "Content-Type: application/json" \
     -d '{"username":"rahim","password":"secret123","fullName":"Rahim Uddin","email":"rahim@kuet.ac.bd","department":"CSE","studentIdNo":"2207117","semester":6}' \
     http://localhost:8080/api/students

# list students
curl -b cookies.txt http://localhost:8080/api/students

# logout
curl -b cookies.txt -X POST http://localhost:8080/api/auth/logout
```

## Notes

- Passwords are stored as BCrypt hashes and never returned by the API.
- CSRF is disabled because this is a cookie-session JSON API used from Postman/curl; enable it if a browser front-end is added.
- `ddl-auto: update` creates the tables automatically; use Flyway/Liquibase for production.
