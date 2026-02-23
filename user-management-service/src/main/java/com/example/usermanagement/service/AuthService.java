package com.example.usermanagement.service;

import com.example.usermanagement.dto.request.LoginRequest;
import com.example.usermanagement.dto.request.RefreshTokenRequest;
import com.example.usermanagement.dto.request.RegisterRequest;
import com.example.usermanagement.dto.response.AuthResponse;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.exception.EmailAlreadyExistsException;
import com.example.usermanagement.exception.UsernameAlreadyExistsException;
import com.example.usermanagement.model.RefreshToken;
import com.example.usermanagement.model.Role;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.RoleRepository;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.security.JwtTokenProvider;
import com.example.usermanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Handles user registration, login, token refresh, and logout.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final UserService userService;

    /**
     * Registers a new user with the default ROLE_USER role.
     *
     * @throws UsernameAlreadyExistsException if username is taken
     * @throws EmailAlreadyExistsException    if email is taken
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_USER not found. Run migrations."));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(Set.of(userRole))
                .enabled(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        user = userRepository.save(user);
        log.info("Registered new user: username='{}', email='{}'", user.getUsername(), user.getEmail());

        return buildAuthResponse(user);
    }

    /**
     * Authenticates a user and issues access + refresh tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

        String accessToken = jwtTokenProvider.generateToken(authentication);
        RefreshToken refreshToken = tokenService.createRefreshToken(user);

        log.info("User '{}' logged in successfully", user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtTokenProvider.getAccessTokenValidityMs() / 1000)
                .user(userService.toUserResponse(user))
                .build();
    }

    /**
     * Issues a new access token using a valid refresh token (rotation strategy).
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken rotated = tokenService.rotateRefreshToken(request.getRefreshToken());
        User user = rotated.getUser();

        UserPrincipal principal = UserPrincipal.of(user);
        String accessToken = jwtTokenProvider.generateToken(principal);

        log.debug("Issued new access token for user '{}'", user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rotated.getToken())
                .expiresIn(jwtTokenProvider.getAccessTokenValidityMs() / 1000)
                .user(userService.toUserResponse(user))
                .build();
    }

    /**
     * Revokes all refresh tokens for the authenticated user.
     */
    @Transactional
    public void logout(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("User not found during logout"));
        tokenService.revokeAllUserTokens(user);
        log.info("User '{}' logged out; all refresh tokens revoked", user.getUsername());
    }

    // ----- Private helpers -----

    private AuthResponse buildAuthResponse(User user) {
        UserPrincipal principal = UserPrincipal.of(user);
        String accessToken = jwtTokenProvider.generateToken(principal);
        RefreshToken refreshToken = tokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtTokenProvider.getAccessTokenValidityMs() / 1000)
                .user(userService.toUserResponse(user))
                .build();
    }
}
