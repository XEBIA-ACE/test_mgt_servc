package com.example.usermanagement.service;

import com.example.usermanagement.dto.request.ChangePasswordRequest;
import com.example.usermanagement.dto.request.UpdateUserRequest;
import com.example.usermanagement.dto.response.PagedResponse;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.exception.EmailAlreadyExistsException;
import com.example.usermanagement.exception.UserNotFoundException;
import com.example.usermanagement.exception.UsernameAlreadyExistsException;
import com.example.usermanagement.model.Role;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.RoleRepository;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for CRUD operations on user accounts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // ----- Queries -----

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return toUserResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal principal) {
        return toUserResponse(findById(principal.getId()));
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(String search, Pageable pageable) {
        Page<User> page = StringUtils.hasText(search)
                ? userRepository.searchUsers(search, pageable)
                : userRepository.findAll(pageable);
        return PagedResponse.of(page.map(this::toUserResponse));
    }

    // ----- Commands -----

    /**
     * Applies a partial update to the user identified by {@code id}.
     * Admins can update any user; regular users can only update themselves.
     */
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request, UserPrincipal currentUser) {
        User user = findById(id);

        boolean isSelf = user.getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isSelf && !isAdmin) {
            throw new AccessDeniedException("You are not permitted to modify this account");
        }

        if (StringUtils.hasText(request.getUsername())
                && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UsernameAlreadyExistsException(request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        if (StringUtils.hasText(request.getEmail())
                && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new EmailAlreadyExistsException(request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName());
        }

        // Only admins may enable/disable accounts
        if (request.getEnabled() != null && isAdmin) {
            user.setEnabled(request.getEnabled());
        }

        user = userRepository.save(user);
        log.info("Updated user '{}'", user.getUsername());
        return toUserResponse(user);
    }

    /** Allows a user to change their own password after verifying the current one. */
    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        User user = findById(principal.getId());

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AccessDeniedException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user '{}'", user.getUsername());
    }

    /** Hard-deletes a user account. Only admins should call this endpoint. */
    @Transactional
    public void deleteUser(UUID id) {
        User user = findById(id);
        userRepository.delete(user);
        log.info("Deleted user '{}' (id={})", user.getUsername(), id);
    }

    /** Assigns an additional role to the user. */
    @Transactional
    public UserResponse assignRole(UUID userId, Role.RoleName roleName) {
        User user = findById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + roleName));
        user.getRoles().add(role);
        user = userRepository.save(user);
        log.info("Assigned role '{}' to user '{}'", roleName, user.getUsername());
        return toUserResponse(user);
    }

    /** Removes a role from the user. */
    @Transactional
    public UserResponse removeRole(UUID userId, Role.RoleName roleName) {
        User user = findById(userId);
        user.getRoles().removeIf(r -> r.getName() == roleName);
        user = userRepository.save(user);
        log.info("Removed role '{}' from user '{}'", roleName, user.getUsername());
        return toUserResponse(user);
    }

    // ----- Mapping -----

    /** Converts a {@link User} entity to its API response DTO. */
    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .accountNonLocked(user.isAccountNonLocked())
                .roles(user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    // ----- Helpers -----

    private User findById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
