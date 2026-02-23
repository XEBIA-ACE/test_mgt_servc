package com.example.usermanagement.service;

import com.example.usermanagement.exception.EmailAlreadyExistsException;
import com.example.usermanagement.exception.InvalidTokenException;
import com.example.usermanagement.model.dto.request.LoginRequest;
import com.example.usermanagement.model.dto.request.RefreshTokenRequest;
import com.example.usermanagement.model.dto.request.RegisterRequest;
import com.example.usermanagement.model.dto.response.AuthResponse;
import com.example.usermanagement.model.entity.RefreshToken;
import com.example.usermanagement.model.entity.User;
import com.example.usermanagement.model.enums.Role;
import com.example.usermanagement.repository.RefreshTokenRepository;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.security.JwtTokenProvider;
import com.example.usermanagement.service.impl.AuthServiceImpl;
import com.example.usermanagement.util.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationDays", 7L);

        testUser = User.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .email("test@example.com")
            .password("encoded-password")
            .role(Role.USER)
            .enabled(true)
            .build();

        testRefreshToken = RefreshToken.builder()
            .id(UUID.randomUUID())
            .token(UUID.randomUUID().toString())
            .user(testUser)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .revoked(false)
            .build();
    }

    // ===== register =====

    @Test
    @DisplayName("register — new user returns auth tokens")
    void register_newUser_returnsAuthResponse() {
        RegisterRequest request = buildRegisterRequest();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        AuthResponse result = authService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo(testRefreshToken.getToken());
        assertThat(result.getExpiresIn()).isEqualTo(900);
    }

    @Test
    @DisplayName("register — duplicate email throws EmailAlreadyExistsException")
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(buildRegisterRequest()))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessageContaining("test@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register — duplicate username throws IllegalArgumentException")
    void register_duplicateUsername_throwsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(buildRegisterRequest()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("testuser");
    }

    // ===== login =====

    @Test
    @DisplayName("login — valid credentials returns auth tokens and revokes old tokens")
    void login_validCredentials_returnsTokensAndRevokesOld() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("Test@1234");

        when(authenticationManager.authenticate(any())).thenReturn(
            new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities())
        );
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any())).thenReturn(testRefreshToken);

        AuthResponse result = authService.login(request);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        // Old tokens must be revoked on login
        verify(refreshTokenRepository).revokeAllUserTokens(testUser);
    }

    // ===== refreshToken =====

    @Test
    @DisplayName("refreshToken — valid token issues new token pair")
    void refreshToken_validToken_returnsNewTokenPair() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(testRefreshToken.getToken());

        when(refreshTokenRepository.findByToken(testRefreshToken.getToken()))
            .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any())).thenReturn(testRefreshToken);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("new-access-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse result = authService.refreshToken(request);

        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        // Old token must be revoked (token rotation)
        assertThat(testRefreshToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("refreshToken — expired token throws InvalidTokenException")
    void refreshToken_expiredToken_throwsException() {
        testRefreshToken = RefreshToken.builder()
            .token("expired-token")
            .user(testUser)
            .expiresAt(LocalDateTime.now().minusDays(1))  // already expired
            .revoked(false)
            .build();

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-token");

        when(refreshTokenRepository.findByToken("expired-token"))
            .thenReturn(Optional.of(testRefreshToken));

        assertThatThrownBy(() -> authService.refreshToken(request))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("refreshToken — non-existent token throws InvalidTokenException")
    void refreshToken_unknownToken_throwsException() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("unknown");

        assertThatThrownBy(() -> authService.refreshToken(request))
            .isInstanceOf(InvalidTokenException.class);
    }

    // ===== logout =====

    @Test
    @DisplayName("logout — valid token is revoked")
    void logout_validToken_revokesToken() {
        when(refreshTokenRepository.findByToken(testRefreshToken.getToken()))
            .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any())).thenReturn(testRefreshToken);

        authService.logout(testRefreshToken.getToken());

        assertThat(testRefreshToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(testRefreshToken);
    }

    @Test
    @DisplayName("logout — unknown token is silently ignored")
    void logout_unknownToken_doesNothing() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        authService.logout("unknown");

        verify(refreshTokenRepository, never()).save(any());
    }

    // ===== Helper =====

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setEmail("test@example.com");
        req.setPassword("Test@1234");
        req.setFirstName("Test");
        req.setLastName("User");
        return req;
    }
}
