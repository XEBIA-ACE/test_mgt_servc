package com.example.usermanagement.service.impl;

import com.example.usermanagement.exception.EmailAlreadyExistsException;
import com.example.usermanagement.exception.InvalidTokenException;
import com.example.usermanagement.model.dto.request.LoginRequest;
import com.example.usermanagement.model.dto.request.RefreshTokenRequest;
import com.example.usermanagement.model.dto.request.RegisterRequest;
import com.example.usermanagement.model.dto.response.AuthResponse;
import com.example.usermanagement.model.entity.RefreshToken;
import com.example.usermanagement.model.entity.User;
import com.example.usermanagement.repository.RefreshTokenRepository;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.security.JwtTokenProvider;
import com.example.usermanagement.service.AuthService;
import com.example.usermanagement.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Value("${app.jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .build();

        User saved = userRepository.save(user);
        log.info("Registered new user: {} ({})", saved.getId(), saved.getUsername());
        return buildAuthResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Delegates to DaoAuthenticationProvider — throws BadCredentialsException on failure
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalStateException("User not found after successful authentication"));

        // Token rotation: revoke all existing refresh tokens before issuing a new one
        refreshTokenRepository.revokeAllUserTokens(user);

        log.info("User logged in: {}", user.getId());
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (!storedToken.isValid()) {
            throw new InvalidTokenException("Refresh token is expired or has been revoked");
        }

        // Rotate: revoke the consumed token and issue a fresh pair
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        log.info("Rotated refresh token for user: {}", user.getId());
        return buildAuthResponse(user);
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Logged out user: {}", token.getUser().getId());
        });
    }

    // ===== Private helpers =====

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken.getToken())
            .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
            .user(userMapper.toUserResponse(user))
            .build();
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
            .token(UUID.randomUUID().toString())
            .user(user)
            .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays))
            .build();
        return refreshTokenRepository.save(refreshToken);
    }
}
