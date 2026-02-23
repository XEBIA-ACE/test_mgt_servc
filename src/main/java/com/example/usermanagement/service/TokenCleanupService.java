package com.example.usermanagement.service;

import com.example.usermanagement.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Periodic job that removes expired and revoked refresh tokens
 * to keep the refresh_tokens table from growing indefinitely.
 */
@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    // Runs every night at 02:00 UTC
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredAndRevokedTokens(Instant.now());
        log.info("Token cleanup completed: deleted {} expired/revoked tokens", deleted);
    }
}
