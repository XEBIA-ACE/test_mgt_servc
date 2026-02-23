package com.example.usermanagement.util;

import com.example.usermanagement.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Utility methods for accessing the Spring Security context.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class — no instances
    }

    /** Returns the currently authenticated {@link UserPrincipal}, if any. */
    public static Optional<UserPrincipal> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    /** Returns the username of the currently authenticated user, or "anonymous". */
    public static String getCurrentUsername() {
        return getCurrentUser()
                .map(UserPrincipal::getUsername)
                .orElse("anonymous");
    }

    /** Returns true if the current user has the given role. */
    public static boolean hasRole(String role) {
        return getCurrentUser()
                .map(u -> u.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals(role)))
                .orElse(false);
    }
}
