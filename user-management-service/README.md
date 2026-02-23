# User Management Service

A production-ready RESTful microservice for user account management built with **Java 21**, **Spring Boot 3.2**, **OAuth 2.0 / JWT** authentication, and **PostgreSQL**.

---

## Table of Contents

- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Quick Start (Docker Compose)](#quick-start-docker-compose)
- [Local Development (without Docker)](#local-development-without-docker)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Authentication Flow](#authentication-flow)
- [Database Migrations](#database-migrations)
- [Testing](#testing)
- [Security Considerations](#security-considerations)
- [Project Structure](#project-structure)

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   HTTP Request                          │
└──────────────────────────┬──────────────────────────────┘
                           │
              ┌────────────▼────────────┐
              │  JwtAuthenticationFilter │  (validates Bearer token)
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │      Controllers        │  (AuthController, UserController, AdminController)
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │       Services          │  (AuthService, UserService, TokenService)
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │     Repositories        │  (Spring Data JPA)
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │      PostgreSQL         │  (Flyway migrations)
              └─────────────────────────┘
```

**Layer responsibilities:**

| Layer | Package | Responsibility |
|---|---|---|
| API | `controller` | HTTP routing, request/response mapping, validation |
| Business Logic | `service` | Domain logic, orchestration, authorization checks |
| Data Access | `repository` | Spring Data JPA queries |
| Security | `security` | JWT generation/validation, Spring Security integration |
| Config | `config` | Security filter chain, Swagger, JPA auditing |

---

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security 6 + JWT (JJWT 0.12) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Migrations | Flyway 10 |
| API Docs | Springdoc OpenAPI 2 (Swagger UI) |
| Build | Maven 3.9 |
| Container | Docker + Docker Compose |
| Testing | JUnit 5, Mockito, Spring MVC Test |

---

## Prerequisites

- Java 21+
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- Docker & Docker Compose (for the easy path)
- PostgreSQL 14+ (if running without Docker)

---

## Quick Start (Docker Compose)

```bash
# 1. Clone and enter the project
git clone <repo-url>
cd user-management-service

# 2. Copy and review environment variables
cp .env.example .env
# Edit .env — at minimum, set APP_JWT_SECRET to a real 512-bit key:
#   openssl rand -base64 64

# 3. Start the stack (PostgreSQL + application)
docker compose up -d

# 4. Check health
curl http://localhost:8080/actuator/health

# 5. Open Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## Local Development (without Docker)

### 1. Start PostgreSQL

```bash
docker run -d \
  --name ums-postgres \
  -e POSTGRES_DB=user_mgmt_dev \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. Generate a JWT secret

```bash
openssl rand -base64 64
```

### 3. Set environment variables

```bash
export APP_JWT_SECRET="<output from step 2>"
export DB_HOST=localhost
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export DB_NAME=user_mgmt_dev
```

### 4. Run the application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Configuration

All sensitive values are injected via environment variables. See [.env.example](.env.example) for the full list.

| Variable | Default | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active profile (`dev`, `staging`, `prod`) |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `user_mgmt_dev` | Database name |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | — | Database password |
| `APP_JWT_SECRET` | — | **Required.** Base64-encoded 512-bit HMAC key |
| `APP_JWT_ACCESS_TOKEN_EXPIRATION_MS` | `900000` | Access token TTL (ms) — default 15 min |
| `APP_JWT_REFRESH_TOKEN_EXPIRATION_MS` | `2592000000` | Refresh token TTL (ms) — default 30 days |

---

## API Reference

Swagger UI is available at **`http://localhost:8080/swagger-ui.html`** when the application is running.

### Authentication Endpoints

| Method | Path | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a new account | No |
| `POST` | `/api/v1/auth/login` | Login and receive tokens | No |
| `POST` | `/api/v1/auth/refresh` | Refresh access token | No |
| `POST` | `/api/v1/auth/logout` | Revoke refresh tokens | Yes |

### User Endpoints

| Method | Path | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/v1/users/me` | Get current user profile | Yes |
| `PUT` | `/api/v1/users/me` | Update current user profile | Yes |
| `POST` | `/api/v1/users/me/change-password` | Change password | Yes |
| `GET` | `/api/v1/users` | List all users | Admin only |
| `GET` | `/api/v1/users/{id}` | Get user by ID | Admin or self |
| `PUT` | `/api/v1/users/{id}` | Update any user | Admin only |
| `DELETE` | `/api/v1/users/{id}` | Delete user | Admin only |

### Admin Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/admin/users/{id}/roles/{roleName}` | Assign role to user |
| `DELETE` | `/api/v1/admin/users/{id}/roles/{roleName}` | Remove role from user |
| `POST` | `/api/v1/admin/users/{id}/lock` | Lock user account |
| `POST` | `/api/v1/admin/users/{id}/unlock` | Unlock user account |

### Health & Metrics

| Path | Description |
|---|---|
| `/actuator/health` | Liveness/readiness check |
| `/actuator/metrics` | Application metrics |

---

## Authentication Flow

```
Client                          Server
  │                               │
  │  POST /auth/register          │
  │ ─────────────────────────────▶│  Creates user, issues tokens
  │◀─────────────────────────────  │  { access_token, refresh_token }
  │                               │
  │  POST /auth/login             │
  │ ─────────────────────────────▶│  Validates credentials
  │◀─────────────────────────────  │  { access_token, refresh_token }
  │                               │
  │  GET /users/me                │
  │  Authorization: Bearer <jwt>  │
  │ ─────────────────────────────▶│  JwtAuthenticationFilter validates JWT
  │◀─────────────────────────────  │  { user profile }
  │                               │
  │  POST /auth/refresh           │
  │  { refresh_token }            │
  │ ─────────────────────────────▶│  Rotates token (old revoked, new issued)
  │◀─────────────────────────────  │  { new access_token, new refresh_token }
  │                               │
  │  POST /auth/logout            │
  │  Authorization: Bearer <jwt>  │
  │ ─────────────────────────────▶│  All refresh tokens revoked
  │◀─────────────────────────────  │  200 OK
```

**Token types:**
- **Access token** — Short-lived JWT (15 min default). Sent as `Authorization: Bearer <token>`.
- **Refresh token** — Long-lived opaque UUID (30 days default). Stored in DB. Used to issue new access tokens. Rotated on each use.

---

## Database Migrations

Flyway runs automatically on startup. Migration scripts live in `src/main/resources/db/migration/`:

| File | Description |
|---|---|
| `V1__create_users_table.sql` | Users table with UUID PK and indexes |
| `V2__create_roles_table.sql` | Roles table and user_roles join table; seeds default roles |
| `V3__create_refresh_tokens_table.sql` | Refresh token storage with partial index |

---

## Testing

```bash
# Unit tests only
mvn test

# Integration tests (requires no live DB — uses H2)
mvn verify

# All tests with coverage report
mvn verify jacoco:report
open target/site/jacoco/index.html
```

Test structure:

```
src/test/java/com/example/usermanagement/
├── controller/
│   └── AuthControllerTest.java          # MockMvc slice tests
├── service/
│   ├── AuthServiceTest.java             # Pure unit tests (Mockito)
│   └── UserServiceTest.java
├── security/
│   └── JwtTokenProviderTest.java
└── integration/
    └── UserManagementIntegrationTest.java  # Full-stack with H2
```

---

## Security Considerations

| Concern | Implementation |
|---|---|
| Password storage | BCrypt (strength 12) |
| Token signing | HMAC-SHA512 (512-bit key minimum) |
| Refresh token reuse | Rotation strategy — consumed tokens are immediately revoked |
| CSRF | Disabled (stateless JWT API; not applicable) |
| SQL injection | Spring Data JPA parameterised queries |
| Input validation | Bean Validation (`@Valid`) on all request bodies |
| Secrets | Environment variables only — never hardcoded |
| Running as root | Container runs as non-root `appuser` |

**Production checklist:**
- [ ] Replace the dev JWT secret with `openssl rand -base64 64`
- [ ] Store secrets in a secrets manager (AWS Secrets Manager, HashiCorp Vault)
- [ ] Restrict CORS `allowedOriginPatterns` in `SecurityConfig`
- [ ] Enable TLS/HTTPS on the load balancer or reverse proxy
- [ ] Set `spring.jpa.show-sql=false` (already off in staging/prod profiles)
- [ ] Review and tighten actuator endpoint exposure

---

## Project Structure

```
user-management-service/
├── src/
│   ├── main/
│   │   ├── java/com/example/usermanagement/
│   │   │   ├── UserManagementApplication.java
│   │   │   ├── config/
│   │   │   │   ├── AuditConfig.java          # JPA auditing + scheduling
│   │   │   │   ├── SecurityConfig.java        # Spring Security filter chain
│   │   │   │   └── SwaggerConfig.java         # OpenAPI definition
│   │   │   ├── controller/
│   │   │   │   ├── AdminController.java
│   │   │   │   ├── AuthController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto/
│   │   │   │   ├── request/                   # Validated inbound payloads
│   │   │   │   └── response/                  # Outbound response envelopes
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── model/
│   │   │   │   ├── RefreshToken.java
│   │   │   │   ├── Role.java
│   │   │   │   └── User.java
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthenticationEntryPoint.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── UserDetailsServiceImpl.java
│   │   │   │   └── UserPrincipal.java
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── TokenService.java
│   │   │   │   └── UserService.java
│   │   │   └── util/
│   │   │       └── SecurityUtils.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-staging.yml
│   │       ├── application-prod.yml
│   │       ├── application-test.yml
│   │       └── db/migration/
│   └── test/
│       └── java/com/example/usermanagement/
│           ├── controller/
│           ├── integration/
│           ├── security/
│           └── service/
├── .env.example
├── .gitignore
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```
