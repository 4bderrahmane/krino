package com.krino.backend.service;

import com.krino.backend.service.email.EmailVerificationTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
