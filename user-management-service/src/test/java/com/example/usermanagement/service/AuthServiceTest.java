package com.example.usermanagement.service;

import com.example.usermanagement.dto.request.RegisterRequest;
import com.example.usermanagement.dto.response.AuthResponse;
import com.example.usermanagement.exception.EmailAlreadyExistsException;
import com.example.usermanagement.exception.UsernameAlreadyExistsException;
import com.example.usermanagement.model.RefreshToken;
import com.example.usermanagement.model.Role;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.RoleRepository;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenService tokenService;
    @Mock private UserService userService;

    @InjectMocks
    private AuthService authService;

    private Role userRole;
    private User savedUser;

    @BeforeEach
    void setUp() {
        userRole = Role.builder().id(1L).name(Role.RoleName.ROLE_USER).build();
        savedUser = User.builder()
                .id(UUID.randomUUID())
                .username("new_user")
                .email("new@example.com")
                .password("$2a$12$encoded")
                .roles(Set.of(userRole))
                .build();
    }

    @Test
    @DisplayName("register succeeds when username and email are unique")
    void register_uniqueCredentials_returnsAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new_user");
        request.setEmail("new@example.com");
        request.setPassword("S3cur3P@ss!");
        request.setFirstName("New");
        request.setLastName("User");

        when(userRepository.existsByUsername("new_user")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(any())).thenReturn("test.jwt.token");
        when(jwtTokenProvider.getAccessTokenValidityMs()).thenReturn(900000L);
        when(tokenService.createRefreshToken(any())).thenReturn(
                RefreshToken.builder()
                        .token(UUID.randomUUID().toString())
                        .user(savedUser)
                        .expiryDate(Instant.now().plusSeconds(86400))
                        .build()
        );
        when(userService.toUserResponse(any())).thenReturn(null);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("test.jwt.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register throws UsernameAlreadyExistsException when username is taken")
    void register_duplicateUsername_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing_user");
        request.setEmail("new@example.com");
        request.setPassword("S3cur3P@ss!");

        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register throws EmailAlreadyExistsException when email is taken")
    void register_duplicateEmail_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new_user");
        request.setEmail("existing@example.com");
        request.setPassword("S3cur3P@ss!");

        when(userRepository.existsByUsername("new_user")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }
}
