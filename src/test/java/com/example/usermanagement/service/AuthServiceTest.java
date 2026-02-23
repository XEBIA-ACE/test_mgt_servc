package com.example.usermanagement.service;

import com.example.usermanagement.config.JwtConfig;
import com.example.usermanagement.dto.request.RegisterRequest;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.exception.DuplicateEmailException;
import com.example.usermanagement.exception.DuplicateUsernameException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.model.Role;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.RefreshTokenRepository;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock JwtConfig jwtConfig;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserDetailsService userDetailsService;
    @Mock UserMapper userMapper;

    @InjectMocks
    AuthService authService;

    private RegisterRequest validRequest;
    private Role userRole;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setEmail("jane@example.com");
        validRequest.setUsername("jane_doe");
        validRequest.setPassword("Secret@123");
        validRequest.setFirstName("Jane");
        validRequest.setLastName("Doe");

        userRole = new Role();
        userRole.setId(1L);
        userRole.setName(Role.RoleName.ROLE_USER);
    }

    @Test
    @DisplayName("register() creates user when email and username are unique")
    void register_success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        User savedUser = User.builder()
            .id(UUID.randomUUID())
            .email(validRequest.getEmail())
            .username(validRequest.getUsername())
            .passwordHash("hashed_password")
            .roles(Set.of(userRole))
            .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse expectedResponse = UserResponse.builder()
            .id(savedUser.getId())
            .email(savedUser.getEmail())
            .username(savedUser.getUsername())
            .build();
        when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

        UserResponse result = authService.register(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(validRequest.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register() throws DuplicateEmailException when email already exists")
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRequest))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessageContaining(validRequest.getEmail());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() throws DuplicateUsernameException when username already taken")
    void register_duplicateUsername_throws() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(validRequest.getUsername())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRequest))
            .isInstanceOf(DuplicateUsernameException.class)
            .hasMessageContaining(validRequest.getUsername());

        verify(userRepository, never()).save(any());
    }
}
