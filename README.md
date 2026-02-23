# User Management Service

A production-ready REST API for user management with JWT-based authentication, role-based access control, and refresh token rotation.

## Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (JJWT 0.12) |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Documentation | SpringDoc OpenAPI 3 (Swagger UI) |
| Mapping | MapStruct |
| Build | Maven |
| Runtime | Docker (multi-stage, non-root) |

---

## Quick Start (Docker Compose)

```bash
# 1. Clone the repository
git clone <repo-url>
cd user-management-service

# 2. Copy and review environment config
cp .env.example .env

# 3. Start PostgreSQL + the application
docker compose up --build

# 4. Verify the service is healthy
curl http://localhost:8080/actuator/health

# 5. Open the interactive API docs
open http://localhost:8080/swagger-ui.html
```

> **Optional:** start PgAdmin alongside:
> ```bash
> docker compose --profile tools up
> # then open http://localhost:5050 (admin@example.com / admin)
> ```

---

## Local Development (without Docker app container)

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 16 running locally (or use `docker compose up postgres`)

```bash
# Start only the database
docker compose up postgres -d

# Copy env and edit if needed
cp .env.example .env

# Run with dev profile (reads application-dev.yml)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile |
| `PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/user_mgmt` | JDBC connection string |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | — | Database password |
| `DB_POOL_SIZE` | `10` | HikariCP max pool size |
| `JWT_SECRET` | — | **Required.** Base64-encoded 256-bit secret |
| `JWT_ISSUER` | `user-management-service` | JWT `iss` claim value |
| `JWT_ACCESS_EXPIRY_MS` | `900000` | Access token TTL (ms) |
| `JWT_REFRESH_EXPIRY_MS` | `604800000` | Refresh token TTL (ms) |

Generate a secure JWT secret:
```bash
openssl rand -base64 64
```

---

## API Reference

Base URL: `http://localhost:8080`

Interactive docs: `GET /swagger-ui.html`

### Authentication

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Register a new user |
| `POST` | `/api/v1/auth/login` | Public | Login, receive access + refresh tokens |
| `POST` | `/api/v1/auth/refresh` | Public | Rotate tokens using a refresh token |
| `POST` | `/api/v1/auth/logout` | Public | Revoke a refresh token |

**Register**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "jane@example.com",
    "username": "jane_doe",
    "password": "Secret@123",
    "firstName": "Jane",
    "lastName": "Doe"
  }'
```

**Login**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"usernameOrEmail": "jane_doe", "password": "Secret@123"}'
```

### Users

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/users` | Bearer | List users (paginated, searchable) |
| `GET` | `/api/v1/users/me` | Bearer | Get current user profile |
| `GET` | `/api/v1/users/{id}` | Bearer | Get user by ID |
| `PATCH` | `/api/v1/users/{id}` | Bearer | Update profile (self or admin) |
| `PATCH` | `/api/v1/users/{id}/password` | Bearer | Change password (self or admin) |

**List users with search**
```bash
curl "http://localhost:8080/api/v1/users?q=jane&page=0&size=10" \
  -H 'Authorization: Bearer <access_token>'
```

### Admin

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/admin/users/{id}/disable` | ADMIN | Disable a user account |
| `POST` | `/api/v1/admin/users/{id}/enable` | ADMIN | Enable / unlock a user account |
| `POST` | `/api/v1/admin/users/{id}/roles/{roleName}` | ADMIN | Assign a role |
| `DELETE` | `/api/v1/admin/users/{id}/roles/{roleName}` | ADMIN | Remove a role |

Valid role names: `ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`

### Health & Observability

| Path | Description |
|---|---|
| `GET /api/v1/ping` | Simple liveness probe |
| `GET /actuator/health` | Detailed health (Spring Actuator) |
| `GET /actuator/metrics` | Metrics (ADMIN only) |
| `GET /actuator/prometheus` | Prometheus scrape endpoint |

---

## Project Structure

```
src/main/java/com/example/usermanagement/
├── UserManagementApplication.java   # Entry point
├── config/                          # Security, JWT, Swagger, Audit config
├── controller/                      # REST controllers (Auth, User, Admin, Health)
├── dto/
│   ├── request/                     # Validated inbound DTOs
│   └── response/                    # Outbound DTOs (never expose entities)
├── exception/                       # Custom exceptions + GlobalExceptionHandler
├── mapper/                          # MapStruct entity ↔ DTO mappers
├── model/                           # JPA entities (User, Role, RefreshToken)
├── repository/                      # Spring Data JPA repositories
├── security/                        # JWT filter, token provider, UserDetailsService
└── service/                         # Business logic (Auth, User, TokenCleanup)

src/main/resources/
├── application.yml                  # Shared config
├── application-{dev,staging,prod}.yml
└── db/migration/                    # Flyway SQL scripts (V1–V4)
```

---

## Security Highlights

- **Passwords** hashed with BCrypt (cost factor 12)
- **JWT access tokens** (HS256, 15-min TTL by default)
- **Refresh token rotation** — old token revoked on every refresh
- **Account lockout** after 5 consecutive failed logins
- **Stateless sessions** — no HTTP session, pure JWT
- **Role-based access control** via `@PreAuthorize` + `@EnableMethodSecurity`
- Swagger UI **disabled in production** profile
- All sensitive values read from **environment variables**

---

## Running Tests

```bash
# Unit + integration tests (uses H2 in-memory DB for speed)
./mvnw test

# Run only integration tests (requires Docker for Testcontainers)
./mvnw verify -P integration-tests
```

---

## Database Migrations

Flyway runs automatically on startup. Scripts live in `src/main/resources/db/migration/`:

| Version | Description |
|---|---|
| V1 | Create `roles` table + seed default roles |
| V2 | Create `users` table with audit columns |
| V3 | Create `user_roles` join table |
| V4 | Create `refresh_tokens` table |

---

## Architecture Overview

```
Client
  │
  ▼
JwtAuthenticationFilter      ← validates Bearer token, sets SecurityContext
  │
  ▼
Spring Security FilterChain  ← enforces URL rules + method-level @PreAuthorize
  │
  ▼
Controller Layer             ← input validation (@Valid), HTTP status codes
  │
  ▼
Service Layer                ← business logic, transaction boundaries
  │
  ▼
Repository Layer             ← Spring Data JPA + custom @Query
  │
  ▼
PostgreSQL                   ← schema managed by Flyway
```
