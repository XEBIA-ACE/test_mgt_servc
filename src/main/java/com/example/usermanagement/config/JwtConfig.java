package com.example.usermanagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {

    /** Secret key used to sign JWT tokens. Must be at least 256 bits (32 chars). */
    private String secret;

    /** Access token validity in milliseconds (default: 15 minutes). */
    private long accessTokenExpirationMs = 900_000L;

    /** Refresh token validity in milliseconds (default: 7 days). */
    private long refreshTokenExpirationMs = 604_800_000L;

    /** Token issuer claim value. */
    private String issuer = "user-management-service";
}
