package com.example.usermanagement.service;

import com.example.usermanagement.exception.UserNotFoundException;
import com.example.usermanagement.model.dto.request.ChangePasswordRequest;
import com.example.usermanagement.model.dto.request.UpdateUserRequest;
import com.example.usermanagement.model.dto.response.PagedResponse;
import com.example.usermanagement.model.dto.response.UserResponse;
import com.example.usermanagement.model.entity.User;
import com.example.usermanagement.model.enums.Role;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.service.impl.UserServiceImpl;
import com.example.usermanagement.util.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User testUser;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
            .id(userId)
            .username("testuser")
            .email("test@example.com")
            .password("encoded-password")
            .firstName("Test")
            .lastName("User")
            .role(Role.USER)
            .enabled(true)
            .build();

        testUserResponse = UserResponse.builder()
            .id(userId)
            .username("testuser")
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .role(Role.USER)
            .enabled(true)
            .build();
    }

    // ===== getUserById =====

    @Test
    @DisplayName("getUserById — existing ID returns mapped response")
    void getUserById_existingId_returnsResponse() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        UserResponse result = userService.getUserById(userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("getUserById — non-existing ID throws UserNotFoundException")
    void getUserById_nonExistingId_throwsNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining(userId.toString());
    }

    // ===== getAllUsers =====

    @Test
    @DisplayName("getAllUsers — no search returns all users paged")
    void getAllUsers_noSearch_returnsPagedResult() {
        Page<User> page = new PageImpl<>(List.of(testUser), PageRequest.of(0, 20), 1);
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        PagedResponse<UserResponse> result = userService.getAllUsers(0, 20, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("getAllUsers — with search uses searchUsers query")
    void getAllUsers_withSearch_callsSearchQuery() {
        Page<User> page = new PageImpl<>(List.of(testUser), PageRequest.of(0, 20), 1);
        when(userRepository.searchUsers(eq("test"), any())).thenReturn(page);
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        PagedResponse<UserResponse> result = userService.getAllUsers(0, 20, "test");

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).searchUsers(eq("test"), any());
        verify(userRepository, never()).findAll(any(PageRequest.class));
    }

    // ===== updateUser =====

    @Test
    @DisplayName("updateUser — updates provided fields only")
    void updateUser_validRequest_updatesFields() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        userService.updateUser(userId, request);

        assertThat(testUser.getFirstName()).isEqualTo("Updated");
        assertThat(testUser.getLastName()).isEqualTo("Name");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateUser — duplicate username throws IllegalArgumentException")
    void updateUser_duplicateUsername_throwsException() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("taken");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(userId, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("taken");
    }

    // ===== deleteUser =====

    @Test
    @DisplayName("deleteUser — existing user is deleted")
    void deleteUser_existingUser_callsDelete() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        userService.deleteUser(userId);

        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("deleteUser — non-existing user throws UserNotFoundException")
    void deleteUser_nonExistingUser_throwsNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(userId))
            .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).delete(any());
    }

    // ===== changePassword =====

    @Test
    @DisplayName("changePassword — correct current password updates password")
    void changePassword_correctPassword_succeeds() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass@1");
        request.setNewPassword("NewPass@1");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPass@1", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("NewPass@1")).thenReturn("new-encoded-password");
        when(userRepository.save(any())).thenReturn(testUser);

        userService.changePassword(userId, request);

        assertThat(testUser.getPassword()).isEqualTo("new-encoded-password");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("changePassword — wrong current password throws IllegalArgumentException")
    void changePassword_wrongPassword_throwsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("WrongPass@1");
        request.setNewPassword("NewPass@1");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPass@1", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(userId, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("incorrect");

        verify(userRepository, never()).save(any());
    }

    // ===== updateUserRole =====

    @Test
    @DisplayName("updateUserRole — valid role updates user")
    void updateUserRole_validRole_updatesRole() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        userService.updateUserRole(userId, "ADMIN");

        assertThat(testUser.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("updateUserRole — invalid role throws IllegalArgumentException")
    void updateUserRole_invalidRole_throwsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.updateUserRole(userId, "SUPERUSER"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid role");
    }
}
