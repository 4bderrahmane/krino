package com.krino.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Purges dead auth tokens so the three token tables don't grow without bound: every login
 * leaves a refresh-token row for 30 days, and reset/verification tokens outlive their use.
 * A daily sweep is plenty — expired rows are already unusable, this only reclaims storage.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupJob {

    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailVerificationTokenService emailVerificationTokenService;

    @Scheduled(cron = "${app.token-cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void purgeDeadTokens() {
        refreshTokenService.cleanupExpiredAndRevokedTokens();
        passwordResetTokenService.deleteExpired();
        emailVerificationTokenService.deleteExpired();
        log.info("Purged expired/revoked auth tokens.");
    }
}
