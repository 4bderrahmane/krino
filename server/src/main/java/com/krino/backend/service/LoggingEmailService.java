package com.krino.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Dev-only {@link EmailService} that prints the mail's actionable content (verification/reset
 * links, initial passwords) to the server console instead of sending anything, so local test
 * accounts with fake addresses can complete email-driven flows. Active when
 * {@code app.mail.log-only=true} (the dev-profile default); production rejects that flag via
 * {@code ProductionConfigurationValidator}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.log-only", havingValue = "true")
public class LoggingEmailService implements EmailService {

    @Override
    public void sendPasswordReset(String to, String resetLink) {
        logMail("password-reset", to, "reset link       : " + resetLink);
    }

    @Override
    public void sendInitialPassword(String to, String firstName, String password) {
        logMail("account-created", to, "initial password : " + password);
    }

    @Override
    public void sendEmailVerification(String to, String firstName, String verificationLink) {
        logMail("verify-email", to, "verification link: " + verificationLink);
    }

    private void logMail(String template, String to, String content) {
        log.info("""

                        ==================== DEV MAIL (not actually sent) ====================
                          template : {}
                          to       : {}
                          {}
                        ======================================================================""",
                template, to, content);
    }
}
