package com.example.usermanagement.service;

import com.example.usermanagement.exception.InvalidTokenException;
import com.example.usermanagement.model.RefreshToken;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Manages the lifecycle of opaque refresh tokens:
 * creation, validation, rotation, and revocation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenValidityMs;

    /**
     * Creates a new refresh token for the given user.
     * Uses a cryptographically random UUID as the token value.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshTokenValidityMs))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(token);
    }

    /**
     * Validates and rotates a refresh token.
     * The consumed token is revoked and a new one is issued.
     *
     * @param tokenValue the raw opaque token string
     * @return a freshly issued {@link RefreshToken}
     * @throws InvalidTokenException if the token is unknown, expired, or revoked
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String tokenValue) {
        RefreshToken existing = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(InvalidTokenException::notFound);

        if (existing.isExpired()) {
            // Proactively revoke to prevent further attempts
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            throw InvalidTokenException.expired();
        }

        if (existing.isRevoked()) {
            throw InvalidTokenException.revoked();
        }

        // Revoke the consumed token (rotation strategy)
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        // Issue a replacement token
        return createRefreshToken(existing.getUser());
    }

    /** Revokes all refresh tokens for the user — used on logout. */
    @Transactional
    public void revokeAllUserTokens(User user) {
        int count = refreshTokenRepository.revokeAllByUser(user);
        log.debug("Revoked {} refresh token(s) for user '{}'", count, user.getUsername());
    }

    /**
     * Scheduled cleanup — removes expired tokens every hour.
     * Prevents unbounded table growth in long-running deployments.
     */
    @Scheduled(fixedRateString = "${app.token.cleanup-interval-ms:3600000}")
    @Transactional
    public void purgeExpiredTokens() {
        int deleted = refreshTokenRepository.deleteAllExpiredBefore(Instant.now());
        if (deleted > 0) {
            log.info("Purged {} expired refresh token(s)", deleted);
        }
    }
}
