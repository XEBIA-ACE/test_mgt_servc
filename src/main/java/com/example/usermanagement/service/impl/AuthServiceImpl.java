package com.example.usermanagement.service.impl;

import com.example.usermanagement.config.JwtProperties;
import com.example.usermanagement.dto.request.LoginRequest;
import com.example.usermanagement.dto.request.RefreshTokenRequest;
import com.example.usermanagement.dto.response.AuthResponse;
import com.example.usermanagement.exception.InvalidTokenException;
import com.example.usermanagement.mapper.UserMapper;
import com.example.usermanagement.model.entity.RefreshToken;
import com.example.usermanagement.repository.RefreshTokenRepository;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.security.JwtTokenProvider;
import com.example.usermanagement.security.UserPrincipal;
import com.example.usermanagement.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsernameOrEmail(),
                request.getPassword()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String accessToken  = tokenProvider.generateAccessToken(principal);
        String refreshToken = createRefreshToken(principal);

        log.info("User id={} logged in", principal.getId());
        return buildAuthResponse(accessToken, refreshToken, principal);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (!stored.isValid()) {
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        // Rotate: revoke old token and issue a new pair
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        UserPrincipal principal = UserPrincipal.of(stored.getUser());
        String accessToken  = tokenProvider.generateAccessToken(principal);
        String newRefreshToken = createRefreshToken(principal);

        log.info("Token refreshed for user id={}", principal.getId());
        return buildAuthResponse(accessToken, newRefreshToken, principal);
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
            .ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
                log.info("User id={} logged out", rt.getUser().getId());
            });
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String createRefreshToken(UserPrincipal principal) {
        RefreshToken rt = RefreshToken.builder()
            .token(UUID.randomUUID().toString())
            .user(userRepository.getReferenceById(principal.getId()))
            .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs()))
            .build();
        return refreshTokenRepository.save(rt).getToken();
    }

    private AuthResponse buildAuthResponse(String accessToken,
                                           String refreshToken,
                                           UserPrincipal principal) {
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(tokenProvider.getAccessTokenExpirationSeconds())
            .user(userRepository.findById(principal.getId())
                .map(userMapper::toResponse)
                .orElseThrow())
            .build();
    }
}
