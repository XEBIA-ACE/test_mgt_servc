package com.example.usermanagement.service;

import com.example.usermanagement.dto.request.ChangePasswordRequest;
import com.example.usermanagement.dto.request.UpdateUserRequest;
import com.example.usermanagement.dto.response.PageResponse;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.exception.UserNotFoundException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.model.Role;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.RoleRepository;
import com.example.usermanagement.repository.UserRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String query, Pageable pageable) {
        Page<User> page = StringUtils.hasText(query)
            ? userRepository.searchUsers(query, pageable)
            : userRepository.findByEnabledTrue(pageable);
        return PageResponse.from(page.map(userMapper::toResponse));
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request, String requestingUsername) {
        User user = findById(id);
        ensureSelfOrAdmin(user, requestingUsername);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());

        User saved = userRepository.save(user);
        log.info("User updated: id={}, by={}", id, requestingUsername);
        return userMapper.toResponse(saved);
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request, String requestingUsername) {
        User user = findById(id);
        ensureSelfOrAdmin(user, requestingUsername);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AccessDeniedException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user id={}", id);
    }

    @Transactional
    public void disableUser(UUID id) {
        User user = findById(id);
        user.setEnabled(false);
        userRepository.save(user);
        log.info("User disabled: id={}", id);
    }

    @Transactional
    public void enableUser(UUID id) {
        User user = findById(id);
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.resetFailedLoginAttempts();
        userRepository.save(user);
        log.info("User enabled: id={}", id);
    }

    @Transactional
    public UserResponse assignRole(UUID userId, String roleName) {
        User user = findById(userId);
        Role.RoleName roleEnum = Role.RoleName.valueOf(roleName);
        Role role = roleRepository.findByName(roleEnum)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        user.getRoles().add(role);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse removeRole(UUID userId, String roleName) {
        User user = findById(userId);
        Role.RoleName roleEnum = Role.RoleName.valueOf(roleName);
        user.getRoles().removeIf(r -> r.getName() == roleEnum);
        return userMapper.toResponse(userRepository.save(user));
    }

    private User findById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    /** Allows the operation only if the caller owns the account or has the ADMIN role. */
    private void ensureSelfOrAdmin(User user, String requestingUsername) {
        boolean isSelf  = user.getUsername().equals(requestingUsername);
        boolean isAdmin = user.getRoles().stream()
            .anyMatch(r -> r.getName() == Role.RoleName.ROLE_ADMIN);

        // Check whether requesting principal has admin role via security context
        boolean requesterIsAdmin = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isSelf && !requesterIsAdmin) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
