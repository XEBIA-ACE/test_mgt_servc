# User Management Service

A production-ready REST API service for managing users, built with **Java Spring Boot**, **JWT authentication**, and **role-based access control**. Follows Clean Architecture principles with clear separation between API, business logic, and data access layers.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Local Setup (Docker)](#local-setup-docker)
  - [Local Setup (Manual)](#local-setup-manual)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
  - [Authentication Endpoints](#authentication-endpoints)
  - [User Endpoints](#user-endpoints)
  - [Admin Endpoints](#admin-endpoints)
  - [Health & Metrics Endpoints](#health--metrics-endpoints)
- [Security Model](#security-model)
- [Database Schema](#database-schema)
- [Running Tests](#running-tests)
- [Docker](#docker)

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────┐
│                    API Layer (Controllers)              │
│           AuthController │ UserController               │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│               Business Logic Layer (Services)           │
│           AuthService │ UserService                     │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│              Data Access Layer (Repositories)           │
│        UserRepository │ RefreshTokenRepository          │
└───────────────────────────┬────────────────────────────┘
                            │
                     ┌──────▼──────┐
                     │ PostgreSQL  │
                     └─────────────┘

Cross-cutting concerns:
  - Security:    JwtAuthenticationFilter, SecurityConfig
  - Exceptions:  GlobalExceptionHandler
  - Mapping:     UserMapper (MapStruct)
  - Observability: Spring Actuator + Prometheus
```

---

## Technology Stack

| Category       | Technology                      |
|----------------|---------------------------------|
| Framework      | Spring Boot 3.2                 |
| Language       | Java 17                         |
| Security       | Spring Security + JWT (jjwt)    |
| Database       | PostgreSQL 16                   |
| ORM            | Spring Data JPA / Hibernate     |
| Migrations     | Flyway                          |
| API Docs       | Springdoc OpenAPI (Swagger UI)  |
| Mapping        | MapStruct                       |
| Metrics        | Micrometer + Prometheus         |
| Build          | Maven                           |
| Containerize   | Docker + Docker Compose         |
| Testing        | JUnit 5, Mockito, MockMvc       |

---

## Project Structure

```
user-management-service/
├── src/
│   ├── main/
│   │   ├── java/com/example/usermanagement/
│   │   │   ├── UserManagementApplication.java
│   │   │   ├── config/
│   │   │   │   ├── ApplicationConfig.java     # Beans: auth manager, password encoder
│   │   │   │   ├── SecurityConfig.java        # Security filter chain, CORS
│   │   │   │   ├── SwaggerConfig.java         # OpenAPI definition
│   │   │   │   └── RequestLoggingConfig.java  # HTTP request logging
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java        # /api/v1/auth/**
│   │   │   │   └── UserController.java        # /api/v1/users/**, /api/v1/admin/**
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java           # Interface
│   │   │   │   ├── UserService.java           # Interface
│   │   │   │   └── impl/
│   │   │   │       ├── AuthServiceImpl.java
│   │   │   │       └── UserServiceImpl.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── RefreshTokenRepository.java
│   │   │   ├── model/
│   │   │   │   ├── entity/
│   │   │   │   │   ├── User.java              # Implements UserDetails
│   │   │   │   │   └── RefreshToken.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── request/               # LoginRequest, RegisterRequest, …
│   │   │   │   │   └── response/              # AuthResponse, UserResponse, ApiResponse
│   │   │   │   └── enums/Role.java
│   │   │   ├── security/
│   │   │   │   ├── JwtTokenProvider.java      # JWT creation & validation
│   │   │   │   └── JwtAuthenticationFilter.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── *.java (custom exceptions)
│   │   │   ├── util/UserMapper.java           # MapStruct entity→DTO
│   │   │   └── task/TokenCleanupTask.java     # Scheduled DB cleanup
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           ├── V1__create_users_table.sql
│   │           └── V2__create_refresh_tokens_table.sql
│   └── test/
│       ├── java/com/example/usermanagement/
│       │   ├── controller/
│       │   │   ├── AuthControllerTest.java
│       │   │   └── UserControllerTest.java
│       │   └── service/
│       │       ├── AuthServiceTest.java
│       │       └── UserServiceTest.java
│       └── resources/application-test.yml
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── .env.example
└── .gitignore
```

---

## Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.9+**
- **Docker & Docker Compose** (for containerized setup)
- **PostgreSQL 16+** (for manual setup)

### Local Setup (Docker)

The fastest way to get running:

```bash
# 1. Clone and enter the project
git clone <repo-url>
cd user-management-service

# 2. Copy and configure environment variables
cp .env.example .env
# Edit .env — set DB_PASSWORD and JWT_SECRET at minimum

# 3. Start all services (app + postgres)
docker compose up -d

# 4. Check logs
docker compose logs -f app

# Service is available at: http://localhost:8080
# Swagger UI:              http://localhost:8080/swagger-ui.html
```

To also start pgAdmin (DB GUI):
```bash
docker compose --profile tools up -d
# pgAdmin available at: http://localhost:5050
# Email: admin@example.com / Password: admin
```

### Local Setup (Manual)

```bash
# 1. Generate Maven wrapper (if not already present)
mvn wrapper:wrapper

# 2. Create the PostgreSQL database
psql -U postgres -c "CREATE DATABASE user_management_dev;"

# 3. Copy and configure environment
cp .env.example .env
# Set DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET

# 4. Export environment variables (or use your IDE's run config)
export $(cat .env | xargs)

# 5. Build and run
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Generate a secure JWT secret:**
```bash
openssl rand -base64 64
```

---

## Configuration

All sensitive values are externalized via environment variables. See `.env.example` for the full list.

| Variable                            | Description                               | Default (dev)  |
|-------------------------------------|-------------------------------------------|----------------|
| `SERVER_PORT`                       | HTTP listen port                          | `8080`         |
| `SPRING_PROFILES_ACTIVE`            | Active Spring profile                     | `dev`          |
| `DB_URL`                            | JDBC URL for PostgreSQL                   | (localhost)    |
| `DB_USERNAME`                       | Database username                         | `postgres`     |
| `DB_PASSWORD`                       | Database password                         | —              |
| `JWT_SECRET`                        | Base64-encoded HMAC-SHA256 signing key    | —              |
| `JWT_ACCESS_TOKEN_EXPIRATION_MS`    | Access token TTL in milliseconds          | `900000` (15m) |
| `JWT_REFRESH_TOKEN_EXPIRATION_DAYS` | Refresh token TTL in days                 | `7`            |

---

## API Documentation

Interactive docs are available at **`/swagger-ui.html`** when running in `dev` profile.

All responses follow the uniform envelope:

```json
{
  "success": true,
  "message": "...",
  "data": { ... },
  "errors": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

---

### Authentication Endpoints

#### `POST /api/v1/auth/register`

Register a new user account. Returns JWT tokens.

**Request:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "Str0ng@Pass",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Password rules:** minimum 8 characters, must include uppercase, lowercase, digit, and special character (`@$!%*?&`).

**Response `201`:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "access_token": "<JWT>",
    "refresh_token": "<UUID>",
    "token_type": "Bearer",
    "expires_in": 900,
    "user": { ... }
  }
}
```

---

#### `POST /api/v1/auth/login`

Authenticate and obtain JWT tokens. Revokes all previous refresh tokens.

**Request:**
```json
{ "email": "john@example.com", "password": "Str0ng@Pass" }
```

**Response `200`:** same structure as register.

---

#### `POST /api/v1/auth/refresh`

Exchange a refresh token for a new access+refresh token pair (token rotation).

**Request:**
```json
{ "refreshToken": "<UUID>" }
```

---

#### `POST /api/v1/auth/logout`

Revoke the given refresh token.

**Request:**
```json
{ "refreshToken": "<UUID>" }
```

---

### User Endpoints

All require `Authorization: Bearer <token>`.

| Method   | Path                         | Description                     |
|----------|------------------------------|---------------------------------|
| `GET`    | `/api/v1/users/me`           | Get current user's profile      |
| `PUT`    | `/api/v1/users/me`           | Update current user's profile   |
| `POST`   | `/api/v1/users/me/change-password` | Change current user's password |

**Update profile request:**
```json
{
  "firstName": "Johnny",
  "lastName": "Doe",
  "username": "johnny_d"
}
```

**Change password request:**
```json
{
  "currentPassword": "OldPass@1",
  "newPassword": "NewPass@2"
}
```

---

### Admin Endpoints

Require `ROLE_ADMIN`. Pass `Authorization: Bearer <admin-JWT>`.

| Method    | Path                            | Description                    |
|-----------|---------------------------------|--------------------------------|
| `GET`     | `/api/v1/admin/users`           | List users (paginated, search) |
| `GET`     | `/api/v1/admin/users/{id}`      | Get user by UUID               |
| `PUT`     | `/api/v1/admin/users/{id}`      | Update user profile            |
| `DELETE`  | `/api/v1/admin/users/{id}`      | Delete user                    |
| `PATCH`   | `/api/v1/admin/users/{id}/role` | Change user role               |

**List users query parameters:**

| Param    | Type    | Default | Description                     |
|----------|---------|---------|---------------------------------|
| `page`   | integer | `0`     | Zero-based page index           |
| `size`   | integer | `20`    | Page size (1–100)               |
| `search` | string  | —       | Filter by name/email/username   |

**Update role:**
```
PATCH /api/v1/admin/users/{id}/role?role=ADMIN
```
Valid roles: `USER`, `MODERATOR`, `ADMIN`.

---

### Health & Metrics Endpoints

| Path                   | Auth Required | Description              |
|------------------------|---------------|--------------------------|
| `GET /actuator/health` | No            | Health status            |
| `GET /actuator/info`   | No            | App version & name       |
| `GET /actuator/metrics`| Yes (any)     | Metrics list             |
| `GET /actuator/prometheus` | ADMIN     | Prometheus scrape target |

---

## Security Model

### JWT Flow

```
Client                          Server
  │                               │
  │── POST /auth/login ──────────►│
  │                               │  Validate credentials
  │◄── access_token (15 min) ─────│  + refresh_token (7 days)
  │                               │
  │── GET /api/... ─────────────►│
  │   Authorization: Bearer <JWT> │  Verify JWT signature & expiry
  │◄── 200 OK ────────────────────│
  │                               │
  │  (access token expires)       │
  │── POST /auth/refresh ────────►│
  │   { refreshToken: "..." }     │  Revoke old → issue new pair
  │◄── new access_token ──────────│  (token rotation)
```

### Key Security Decisions

- **Stateless sessions** — no server-side session state; JWT carries all auth info
- **Token rotation** — each refresh call issues a new pair and revokes the old one
- **BCrypt cost 12** — strong enough for production while keeping login latency reasonable
- **Role-based access** — `USER`, `MODERATOR`, `ADMIN` roles enforced via `@PreAuthorize`
- **Non-root Docker container** — app runs as `appuser` (not root)
- **No secrets in config files** — all sensitive values via environment variables

---

## Database Schema

```sql
users
  id UUID PK
  username VARCHAR(50) UNIQUE NOT NULL
  email VARCHAR(255) UNIQUE NOT NULL
  password VARCHAR(255) NOT NULL
  first_name VARCHAR(100)
  last_name VARCHAR(100)
  role VARCHAR(20) NOT NULL DEFAULT 'USER'
  is_enabled BOOLEAN DEFAULT TRUE
  is_account_non_expired BOOLEAN DEFAULT TRUE
  is_account_non_locked BOOLEAN DEFAULT TRUE
  is_credentials_non_expired BOOLEAN DEFAULT TRUE
  created_at TIMESTAMP
  updated_at TIMESTAMP  ← auto-updated by trigger

refresh_tokens
  id UUID PK
  token VARCHAR(512) UNIQUE NOT NULL
  user_id UUID FK → users(id) ON DELETE CASCADE
  expires_at TIMESTAMP NOT NULL
  revoked BOOLEAN DEFAULT FALSE
  created_at TIMESTAMP
```

Schema is version-controlled via **Flyway** migrations in `src/main/resources/db/migration/`.

---

## Running Tests

```bash
# Unit tests only (fast, no DB required)
./mvnw test -Dspring.profiles.active=test

# All tests with coverage report
./mvnw verify

# Specific test class
./mvnw test -Dtest=UserServiceTest

# Skip tests during build
./mvnw package -DskipTests
```

Tests use an **H2 in-memory database** (PostgreSQL compatibility mode) with Flyway disabled — Hibernate creates the schema via `ddl-auto: create-drop`.

---

## Docker

### Build image
```bash
docker build -t user-management-service .
```

### Run with Docker Compose
```bash
# Start everything
docker compose up -d

# View logs
docker compose logs -f app

# Stop everything
docker compose down

# Stop and remove volumes (clears DB data)
docker compose down -v
```

### Environment variables for Docker
Pass variables via `.env` file or inline:
```bash
JWT_SECRET=$(openssl rand -base64 64) \
DB_PASSWORD=mypassword \
docker compose up -d
```

---

## Development Notes

- **Add an admin user** — insert directly into the DB and set `role = 'ADMIN'`, or register normally and patch the role via SQL
- **Swagger** — available at `http://localhost:8080/swagger-ui.html` in `dev` profile only; disabled in `prod`
- **Scheduled cleanup** — expired/revoked refresh tokens are purged daily at midnight via `TokenCleanupTask`
- **Request logging** — HTTP requests are logged at DEBUG level; toggle in `application.yml`
