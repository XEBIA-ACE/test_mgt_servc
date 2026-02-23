package com.example.usermanagement.service;

import com.example.usermanagement.dto.request.UpdateUserRequest;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.exception.EmailAlreadyExistsException;
import com.example.usermanagement.exception.UserNotFoundException;
import com.example.usermanagement.model.Role;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.RoleRepository;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        Role userRole = Role.builder().id(1L).name(Role.RoleName.ROLE_USER).build();

        testUser = User.builder()
                .id(testUserId)
                .username("john_doe")
                .email("john@example.com")
                .password("$2a$12$hashedpassword")
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .roles(Set.of(userRole))
                .build();

        Role adminRole = Role.builder().id(2L).name(Role.RoleName.ROLE_ADMIN).build();
        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@example.com")
                .password("$2a$12$hashedpassword")
                .roles(Set.of(adminRole))
                .build();
        adminPrincipal = UserPrincipal.of(adminUser);
    }

    @Test
    @DisplayName("getUserById returns UserResponse when user exists")
    void getUserById_existingUser_returnsResponse() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getUserById(testUserId);

        assertThat(response.getId()).isEqualTo(testUserId);
        assertThat(response.getUsername()).isEqualTo("john_doe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("getUserById throws UserNotFoundException when user does not exist")
    void getUserById_nonExistentUser_throwsNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(unknownId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("updateUser changes email when new email is unique")
    void updateUser_uniqueEmail_updatesSuccessfully() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("new@example.com");

        UserResponse response = userService.updateUser(testUserId, request, adminPrincipal);

        assertThat(response).isNotNull();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateUser throws EmailAlreadyExistsException when email is taken")
    void updateUser_duplicateEmail_throwsConflictException() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("taken@example.com");

        assertThatThrownBy(() -> userService.updateUser(testUserId, request, adminPrincipal))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteUser removes the user from the repository")
    void deleteUser_existingUser_deletesSuccessfully() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        userService.deleteUser(testUserId);

        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("toUserResponse maps all fields correctly")
    void toUserResponse_mapsAllFields() {
        UserResponse response = userService.toUserResponse(testUser);

        assertThat(response.getId()).isEqualTo(testUserId);
        assertThat(response.getUsername()).isEqualTo("john_doe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.isEnabled()).isTrue();
        assertThat(response.getRoles()).contains("ROLE_USER");
    }
}
