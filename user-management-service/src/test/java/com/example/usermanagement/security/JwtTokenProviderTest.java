package com.example.usermanagement.security;

import com.example.usermanagement.model.Role;
import com.example.usermanagement.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    // 512-bit test secret (Base64-encoded 64-byte random value)
    private static final String TEST_SECRET =
            Base64.getEncoder().encodeToString(new byte[64]);

    private JwtTokenProvider tokenProvider;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(TEST_SECRET, 900_000L);

        Role userRole = Role.builder().id(1L).name(Role.RoleName.ROLE_USER).build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("test_user")
                .email("test@example.com")
                .password("$2a$12$hashed")
                .roles(Set.of(userRole))
                .enabled(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        testPrincipal = UserPrincipal.of(user);
    }

    @Test
    @DisplayName("generateToken produces a non-blank JWT string")
    void generateToken_returnNonBlankToken() {
        String token = tokenProvider.generateToken(testPrincipal);
        assertThat(token).isNotBlank();
        // A JWT has exactly 3 dot-separated parts
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("validateToken returns true for a freshly issued token")
    void validateToken_freshToken_returnsTrue() {
        String token = tokenProvider.generateToken(testPrincipal);
        assertThat(tokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken returns false for a tampered token")
    void validateToken_tamperedToken_returnsFalse() {
        String token = tokenProvider.generateToken(testPrincipal) + "tampered";
        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("getUsernameFromToken extracts the correct subject")
    void getUsernameFromToken_returnsCorrectUsername() {
        String token = tokenProvider.generateToken(testPrincipal);
        String username = tokenProvider.getUsernameFromToken(token);
        assertThat(username).isEqualTo("test_user");
    }

    @Test
    @DisplayName("validateToken returns false for an empty string")
    void validateToken_emptyString_returnsFalse() {
        assertThat(tokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("getAccessTokenValidityMs returns configured value")
    void getAccessTokenValidityMs_returnsConfiguredValue() {
        assertThat(tokenProvider.getAccessTokenValidityMs()).isEqualTo(900_000L);
    }
}
