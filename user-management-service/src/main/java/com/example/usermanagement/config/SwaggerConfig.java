package com.example.usermanagement.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger configuration.
 * Access the UI at: http://localhost:8080/swagger-ui.html
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "User Management Service API",
                version = "1.0.0",
                description = """
                        REST API for user account management with OAuth 2.0 / JWT authentication.

                        ## Authentication
                        1. **Register** a new account via `POST /api/v1/auth/register`
                        2. **Login** via `POST /api/v1/auth/login` to receive an `access_token`
                        3. Click **Authorize** (top right) and enter `Bearer <access_token>`
                        4. All protected endpoints will now be accessible
                        """,
                contact = @Contact(name = "Platform Team", email = "platform@example.com"),
                license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development"),
                @Server(url = "https://api-staging.example.com", description = "Staging"),
                @Server(url = "https://api.example.com", description = "Production")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Provide the JWT access token obtained from /api/v1/auth/login"
)
public class SwaggerConfig {
    // Configuration is driven by annotations above; no beans required
}
