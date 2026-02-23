package com.example.usermanagement.config;

import com.example.usermanagement.security.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Enables JPA entity auditing (createdAt, updatedAt, createdBy, updatedBy fields)
 * and Spring's scheduling support (used for refresh-token cleanup).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableScheduling
public class AuditConfig {

    /**
     * Provides the current authenticated username to JPA Auditing.
     * Falls back to "system" for operations triggered outside a security context
     * (e.g. scheduled jobs, startup initialization).
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.of("system");
            }
            if (auth.getPrincipal() instanceof UserPrincipal principal) {
                return Optional.of(principal.getUsername());
            }
            return Optional.of(auth.getName());
        };
    }
}
