package com.example.usermanagement.task;

import com.example.usermanagement.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled task that removes expired and revoked refresh tokens from the database.
 * Prevents unbounded table growth over time.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupTask {

    private final RefreshTokenRepository refreshTokenRepository;

    /** Runs every day at midnight (server timezone). */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of expired/revoked refresh tokens");
        refreshTokenRepository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
        log.info("Completed refresh token cleanup");
    }
}
