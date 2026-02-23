package com.example.usermanagement.service.impl;

import com.example.usermanagement.exception.UserNotFoundException;
import com.example.usermanagement.model.dto.request.ChangePasswordRequest;
import com.example.usermanagement.model.dto.request.UpdateUserRequest;
import com.example.usermanagement.model.dto.response.PagedResponse;
import com.example.usermanagement.model.dto.response.UserResponse;
import com.example.usermanagement.model.entity.User;
import com.example.usermanagement.model.enums.Role;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.service.UserService;
import com.example.usermanagement.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userMapper.toUserResponse(findUserById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> userPage = StringUtils.hasText(search)
            ? userRepository.searchUsers(search.trim(), pageable)
            : userRepository.findAll(pageable);

        return PagedResponse.<UserResponse>builder()
            .content(userPage.getContent().stream().map(userMapper::toUserResponse).toList())
            .page(userPage.getNumber())
            .size(userPage.getSize())
            .totalElements(userPage.getTotalElements())
            .totalPages(userPage.getTotalPages())
            .first(userPage.isFirst())
            .last(userPage.isLast())
            .build();
    }

    @Override
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = findUserById(id);

        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName());
        }
        if (StringUtils.hasText(request.getUsername())) {
            boolean usernameChanged = !request.getUsername().equals(user.getDisplayUsername());
            if (usernameChanged && userRepository.existsByUsername(request.getUsername())) {
                throw new IllegalArgumentException("Username already taken: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        User saved = userRepository.save(user);
        log.info("Updated profile for user: {}", saved.getId());
        return userMapper.toUserResponse(saved);
    }

    @Override
    public void deleteUser(UUID id) {
        User user = findUserById(id);
        userRepository.delete(user);
        log.info("Deleted user: {}", id);
    }

    @Override
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = findUserById(id);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Changed password for user: {}", id);
    }

    @Override
    public UserResponse updateUserRole(UUID id, String role) {
        User user = findUserById(id);
        try {
            user.setRole(Role.valueOf(role.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        User saved = userRepository.save(user);
        log.info("Updated role for user {} to {}", id, role);
        return userMapper.toUserResponse(saved);
    }

    // ===== Private helpers =====

    private User findUserById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }
}
