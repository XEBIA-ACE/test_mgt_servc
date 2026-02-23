package com.example.usermanagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized JWT configuration bound from application.yml.
 * All values must be set via environment variables in production.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Base64-encoded HMAC-SHA-256 secret (min 256 bits). */
    private String secret;

    /** Access token validity in milliseconds. */
    private long accessTokenExpirationMs = 900_000; // 15 minutes

    /** Refresh token validity in milliseconds. */
    private long refreshTokenExpirationMs = 604_800_000; // 7 days

    /** Token issuer claim. */
    private String issuer = "user-management-service";
}
