package com.example.usermanagement.service;

import com.example.usermanagement.config.JwtConfig;
import com.example.usermanagement.dto.request.LoginRequest;
import com.example.usermanagement.dto.request.RegisterRequest;
import com.example.usermanagement.dto.response.AuthResponse;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.exception.DuplicateEmailException;
import com.example.usermanagement.exception.DuplicateUsernameException;
import com.example.usermanagement.exception.InvalidTokenException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.model.RefreshToken;
import com.example.usermanagement.model.Role;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.RefreshTokenRepository;
import com.example.usermanagement.repository.RoleRepository;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtConfig jwtConfig;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException("Username already taken: " + request.getUsername());
        }

        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
            .orElseThrow(() -> new IllegalStateException("Default role ROLE_USER not found in database"));

        User user = User.builder()
            .email(request.getEmail())
            .username(request.getUsername())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .roles(Set.of(userRole))
            .build();

        User saved = userRepository.save(user);
        log.info("Registered new user: id={}, username={}", saved.getId(), saved.getUsername());
        return userMapper.toResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Delegates to DaoAuthenticationProvider which calls UserDetailsServiceImpl
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsernameOrEmail(),
                request.getPassword()
            )
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
            .or(() -> userRepository.findByEmail(userDetails.getUsername()))
            .orElseThrow();

        // Reset failed attempts on successful login
        user.resetFailedLoginAttempts();
        userRepository.updateLastLoginAt(user.getId(), Instant.now());

        String accessToken  = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = createRefreshToken(user);

        log.info("User logged in: id={}, username={}", user.getId(), user.getUsername());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(jwtConfig.getAccessTokenExpirationMs() / 1000)
            .user(userMapper.toResponse(user))
            .build();
    }

    @Transactional
    public AuthResponse refreshTokens(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(rawToken)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (stored.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }
        if (stored.isExpired()) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        // Rotate refresh token
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        String newAccessToken  = jwtTokenProvider.generateAccessToken(userDetails);
        String newRefreshToken = createRefreshToken(user);

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .expiresIn(jwtConfig.getAccessTokenExpirationMs() / 1000)
            .user(userMapper.toResponse(user))
            .build();
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokenRepository.findByToken(rawToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Refresh token revoked for user id={}", token.getUser().getId());
        });
    }

    private String createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
            .token(UUID.randomUUID().toString())
            .user(user)
            .expiresAt(Instant.now().plusMillis(jwtConfig.getRefreshTokenExpirationMs()))
            .build();
        return refreshTokenRepository.save(token).getToken();
    }
}
