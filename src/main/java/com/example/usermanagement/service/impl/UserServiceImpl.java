package com.example.usermanagement.service.impl;

import com.example.usermanagement.dto.request.ChangePasswordRequest;
import com.example.usermanagement.dto.request.RegisterRequest;
import com.example.usermanagement.dto.request.UpdateUserRequest;
import com.example.usermanagement.dto.response.PageResponse;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.exception.BadCredentialsException;
import com.example.usermanagement.exception.UserAlreadyExistsException;
import com.example.usermanagement.exception.UserNotFoundException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.model.entity.Role;
import com.example.usermanagement.model.entity.User;
import com.example.usermanagement.repository.RefreshTokenRepository;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("email", request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("username", request.getUsername());
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .roles(Set.of(Role.ROLE_USER))
            .build();

        User saved = userRepository.save(user);
        log.info("Registered new user id={} username={}", saved.getId(), saved.getUsername());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userMapper.toResponse(findUserById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        return userRepository.findByUsername(username)
            .map(userMapper::toResponse)
            .orElseThrow(() -> new UserNotFoundException("username", username));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(userMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String query, Pageable pageable) {
        return PageResponse.from(userRepository.search(query, pageable).map(userMapper::toResponse));
    }

    @Override
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = findUserById(id);

        if (StringUtils.hasText(request.getUsername())
                && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UserAlreadyExistsException("username", request.getUsername());
            }
            user.setUsername(request.getUsername());
        }
        if (StringUtils.hasText(request.getEmail())
                && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("email", request.getEmail());
            }
            user.setEmail(request.getEmail().toLowerCase());
        }
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = findUserById(id);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException();
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens so existing sessions are invalidated after a password change
        refreshTokenRepository.revokeAllByUser(user);
        log.info("Password changed for user id={}", id);
    }

    @Override
    public void assignRole(UUID id, Role role) {
        User user = findUserById(id);
        user.getRoles().add(role);
        userRepository.save(user);
        log.info("Role {} assigned to user id={}", role, id);
    }

    @Override
    public void removeRole(UUID id, Role role) {
        User user = findUserById(id);
        user.getRoles().remove(role);
        userRepository.save(user);
        log.info("Role {} removed from user id={}", role, id);
    }

    @Override
    public void disableUser(UUID id) {
        User user = findUserById(id);
        user.setEnabled(false);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUser(user);
        log.info("User id={} disabled", id);
    }

    @Override
    public void enableUser(UUID id) {
        User user = findUserById(id);
        user.setEnabled(true);
        userRepository.save(user);
        log.info("User id={} enabled", id);
    }

    @Override
    public void deleteUser(UUID id) {
        User user = findUserById(id);
        refreshTokenRepository.revokeAllByUser(user);
        userRepository.delete(user);
        log.info("User id={} deleted", id);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private User findUserById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
}
