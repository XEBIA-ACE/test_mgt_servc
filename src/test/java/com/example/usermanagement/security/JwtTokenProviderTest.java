package com.example.usermanagement.security;

import com.example.usermanagement.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    // 512-bit Base64 test secret (safe length for HS256)
    private static final String TEST_SECRET =
        "dGVzdFNlY3JldEtleUZvclVuaXRUZXN0c09ubHlEb05vdFVzZUluUHJvZHVjdGlvbkVudmlyb25tZW50";

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret(TEST_SECRET);
        config.setAccessTokenExpirationMs(3_600_000L);
        config.setIssuer("test-issuer");
        provider = new JwtTokenProvider(config);
    }

    private UserDetails sampleUser() {
        return User.withUsername("jane_doe")
            .password("irrelevant")
            .authorities(new SimpleGrantedAuthority("ROLE_USER"))
            .build();
    }

    @Test
    @DisplayName("generateAccessToken() produces a non-blank token")
    void generateToken_notBlank() {
        String token = provider.generateAccessToken(sampleUser());
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extractUsername() returns the correct subject")
    void extractUsername_correct() {
        String token = provider.generateAccessToken(sampleUser());
        assertThat(provider.extractUsername(token)).isEqualTo("jane_doe");
    }

    @Test
    @DisplayName("isTokenValid() returns true for a fresh token")
    void isTokenValid_fresh() {
        String token = provider.generateAccessToken(sampleUser());
        assertThat(provider.isTokenValid(token, sampleUser())).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() returns false for wrong user")
    void isTokenValid_wrongUser() {
        String token = provider.generateAccessToken(sampleUser());
        UserDetails other = User.withUsername("other").password("x").authorities(List.of()).build();
        assertThat(provider.isTokenValid(token, other)).isFalse();
    }

    @Test
    @DisplayName("extractRoles() returns the roles embedded in the token")
    void extractRoles() {
        String token = provider.generateAccessToken(sampleUser());
        assertThat(provider.extractRoles(token)).contains("ROLE_USER");
    }
}
