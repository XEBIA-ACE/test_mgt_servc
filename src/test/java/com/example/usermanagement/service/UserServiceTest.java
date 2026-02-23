package com.example.usermanagement.service;

import com.example.usermanagement.dto.request.RegisterRequest;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.exception.UserAlreadyExistsException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.model.entity.Role;
import com.example.usermanagement.model.entity.User;
import com.example.usermanagement.repository.RefreshTokenRepository;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService unit tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserServiceImpl userService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setUsername("johndoe");
        validRequest.setEmail("john@example.com");
        validRequest.setPassword("Secret@1");
        validRequest.setFirstName("John");
        validRequest.setLastName("Doe");
    }

    @Test
    @DisplayName("register - success: new user is persisted and returned")
    void register_success() {
        given(userRepository.existsByEmail(any())).willReturn(false);
        given(userRepository.existsByUsername(any())).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("hashed");

        User saved = User.builder()
            .id(UUID.randomUUID())
            .username("johndoe")
            .email("john@example.com")
            .passwordHash("hashed")
            .roles(Set.of(Role.ROLE_USER))
            .build();
        given(userRepository.save(any(User.class))).willReturn(saved);

        UserResponse expected = UserResponse.builder()
            .id(saved.getId())
            .username("johndoe")
            .email("john@example.com")
            .build();
        given(userMapper.toResponse(saved)).willReturn(expected);

        UserResponse result = userService.register(validRequest);

        assertThat(result.getUsername()).isEqualTo("johndoe");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register - duplicate email throws UserAlreadyExistsException")
    void register_duplicateEmail_throws() {
        given(userRepository.existsByEmail("john@example.com")).willReturn(true);

        assertThatThrownBy(() -> userService.register(validRequest))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - duplicate username throws UserAlreadyExistsException")
    void register_duplicateUsername_throws() {
        given(userRepository.existsByEmail(any())).willReturn(false);
        given(userRepository.existsByUsername("johndoe")).willReturn(true);

        assertThatThrownBy(() -> userService.register(validRequest))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessageContaining("username");

        verify(userRepository, never()).save(any());
    }
}
