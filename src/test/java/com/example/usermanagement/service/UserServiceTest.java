package com.example.usermanagement.service;

import com.example.usermanagement.dto.request.UpdateUserRequest;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.exception.UserNotFoundException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.model.Role;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.RoleRepository;
import com.example.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserMapper userMapper;

    @InjectMocks
    UserService userService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
            .id(testUserId)
            .email("jane@example.com")
            .username("jane_doe")
            .passwordHash("hashed")
            .firstName("Jane")
            .lastName("Doe")
            .enabled(true)
            .roles(Set.of())
            .build();

        // Mock security context with ADMIN authority for update tests
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenAnswer(i ->
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    @DisplayName("getUserById() returns mapped response when user exists")
    void getUserById_found() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        UserResponse expected = UserResponse.builder().id(testUserId).build();
        when(userMapper.toResponse(testUser)).thenReturn(expected);

        UserResponse result = userService.getUserById(testUserId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("getUserById() throws UserNotFoundException when user does not exist")
    void getUserById_notFound() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(testUserId))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("updateUser() persists only non-null fields")
    void updateUser_partialUpdate() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Janet");
        // lastName is null — should not be overwritten

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        UserResponse expected = UserResponse.builder().id(testUserId).firstName("Janet").build();
        when(userMapper.toResponse(testUser)).thenReturn(expected);

        UserResponse result = userService.updateUser(testUserId, request, "admin");

        verify(userRepository).save(argThat(u -> "Janet".equals(u.getFirstName())));
        assertThat(result.getFirstName()).isEqualTo("Janet");
    }

    @Test
    @DisplayName("disableUser() sets enabled=false")
    void disableUser() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.disableUser(testUserId);

        verify(userRepository).save(argThat(u -> !u.isEnabled()));
    }

    @Test
    @DisplayName("enableUser() resets lock and failed attempts")
    void enableUser() {
        testUser.setEnabled(false);
        testUser.setAccountNonLocked(false);
        testUser.incrementFailedLoginAttempts();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.enableUser(testUserId);

        verify(userRepository).save(argThat(u ->
            u.isEnabled() && u.isAccountNonLocked() && u.getFailedLoginAttempts() == 0));
    }
}
