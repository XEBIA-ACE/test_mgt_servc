# User Management Service

A production-ready REST API for user registration, authentication, and profile management built with **Java 21**, **Spring Boot 3**, **OAuth 2.0 / JWT**, and **PostgreSQL**.

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Security](#security)
- [Database](#database)
- [Testing](#testing)
- [Docker](#docker)

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                        HTTP Clients                      │
└─────────────────────────┬────────────────────────────────┘
                          │
              ┌───────────▼────────────┐
              │  JwtAuthenticationFilter│  (stateless auth)
              └───────────┬────────────┘
                          │
         ┌────────────────▼─────────────────┐
         │           Controllers             │
         │  AuthController  UserController   │
         └────────────────┬─────────────────┘
                          │
         ┌────────────────▼─────────────────┐
         │             Services              │
         │  AuthService    UserService       │
         └────────────────┬─────────────────┘
                          │
         ┌────────────────▼─────────────────┐
         │           Repositories            │
         │  UserRepository RefreshTokenRepo  │
         └────────────────┬─────────────────┘
                          │
              ┌───────────▼────────────┐
              │       PostgreSQL        │
              └────────────────────────┘
```

**Layer separation:**
| Layer | Package | Responsibility |
|-------|---------|----------------|
| API | `controller` | HTTP routing, request validation, serialization |
| Business | `service` | Domain logic, orchestration |
| Data | `repository` | JPA data access |
| Security | `security` | JWT, filters, UserDetails |
| Config | `config` | Spring beans, properties |

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (JJWT 0.12) |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Mapping | MapStruct |
| Build | Maven |
| Container | Docker / Docker Compose |

---

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### Option A — Docker Compose (recommended)

```bash
# 1. Clone the repo
git clone https://github.com/example/user-management-service.git
cd user-management-service

# 2. Copy environment file
cp .env.example .env
# Edit .env with your values (especially JWT_SECRET in production)

# 3. Start all services
docker compose up --build

# 4. Explore the API
open http://localhost:8080/swagger-ui.html
```

### Option B — Local Maven

```bash
# Start only PostgreSQL
docker compose up postgres -d

# Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## Configuration

All sensitive values must be supplied via environment variables. Copy `.env.example` to `.env` and fill in:

| Variable | Description | Default |
|----------|-------------|---------|
| `APP_ENV` | Active profile (`dev`/`staging`/`prod`) | `dev` |
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/user_mgmt` |
| `DB_USERNAME` | DB user | `postgres` |
| `DB_PASSWORD` | DB password | `postgres` |
| `JWT_SECRET` | Base64-encoded HMAC-SHA-256 key (≥256 bit) | dev default |
| `JWT_ACCESS_EXPIRY_MS` | Access token TTL in ms | `900000` (15 min) |
| `JWT_REFRESH_EXPIRY_MS` | Refresh token TTL in ms | `604800000` (7 days) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:3000` |

Generate a secure JWT secret:
```bash
openssl rand -base64 64
```

---

## API Reference

Interactive docs: **`http://localhost:8080/swagger-ui.html`**

### Authentication

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/v1/auth/login` | Public | Authenticate; receive access + refresh tokens |
| `POST` | `/api/v1/auth/refresh` | Public | Rotate tokens using a refresh token |
| `POST` | `/api/v1/auth/logout` | Public | Revoke a refresh token |

#### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "john@example.com",
  "password": "Secret@1"
}
```

```json
{
  "success": true,
  "data": {
    "access_token": "eyJ...",
    "refresh_token": "550e8400-...",
    "token_type": "Bearer",
    "expires_in": 900,
    "user": { "id": "...", "username": "john", ... }
  }
}
```

### Users

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/v1/users/register` | Public | Register a new account |
| `GET` | `/api/v1/users/me` | User | Get own profile |
| `PUT` | `/api/v1/users/me` | User | Update own profile |
| `PATCH` | `/api/v1/users/me/password` | User | Change own password |
| `GET` | `/api/v1/users` | Admin | List all users (paginated) |
| `GET` | `/api/v1/users/search?q=` | Admin | Search users |
| `GET` | `/api/v1/users/{id}` | Admin | Get user by ID |
| `PUT` | `/api/v1/users/{id}` | Admin | Update any user |
| `POST` | `/api/v1/users/{id}/roles/{role}` | Admin | Assign a role |
| `DELETE` | `/api/v1/users/{id}/roles/{role}` | Admin | Remove a role |
| `PATCH` | `/api/v1/users/{id}/disable` | Admin | Disable user |
| `PATCH` | `/api/v1/users/{id}/enable` | Admin | Enable user |
| `DELETE` | `/api/v1/users/{id}` | Admin | Delete user |

### Health & Metrics

| Path | Description |
|------|-------------|
| `/api/v1/ping` | Lightweight liveness check |
| `/actuator/health` | Spring Boot health (readiness + liveness) |
| `/actuator/metrics` | Micrometer metrics |
| `/actuator/prometheus` | Prometheus scrape endpoint |

---

## Security

- **JWT (HMAC-SHA-256):** Short-lived access tokens (15 min) paired with long-lived refresh tokens (7 days).
- **Token rotation:** Refresh tokens are single-use; a new pair is issued on every refresh.
- **Server-side revocation:** Refresh tokens are persisted; logout and password changes invalidate all active tokens immediately.
- **BCrypt:** Passwords are hashed with BCrypt (strength 12).
- **Role-based access control:** `ROLE_USER`, `ROLE_ADMIN`, `ROLE_MODERATOR` enforced via `@PreAuthorize`.
- **CORS:** Restricted to configured origins.
- **Input validation:** Bean Validation on all request bodies.

### Default Admin Credentials

A seed admin user is created by migration V2. **Change the password immediately after first login.**

```
Email: admin@example.com
Password: Admin@12345
```

---

## Database

Schema is managed by **Flyway**. Migrations live in `src/main/resources/db/migration/`:

| Version | Description |
|---------|-------------|
| V1 | `users` table |
| V2 | `user_roles` table + seed admin |
| V3 | `refresh_tokens` table |

---

## Testing

```bash
# Unit + integration tests
./mvnw test

# Tests with coverage report
./mvnw verify

# View report
open target/site/jacoco/index.html
```

Test structure:
```
src/test/java/com/example/usermanagement/
├── controller/
│   ├── AuthControllerTest.java    # MockMvc integration tests
│   └── UserControllerTest.java
└── service/
    └── UserServiceTest.java       # Mockito unit tests
```

---

## Docker

```bash
# Start app + PostgreSQL
docker compose up --build

# Start with pgAdmin UI
docker compose --profile tools up

# Rebuild app only
docker compose up app --build

# Stop and remove volumes
docker compose down -v
```

### Multi-stage build

The `Dockerfile` uses a two-stage build:
1. **Builder stage** — JDK 21 Alpine, Maven build
2. **Runtime stage** — JRE 21 Alpine, non-root user, optimised JVM flags

Image size: ~200 MB (JRE + fat jar).
