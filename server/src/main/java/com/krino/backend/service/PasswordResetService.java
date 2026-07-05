package com.krino.backend.service;

import com.krino.backend.entity.PasswordResetToken;
import com.krino.backend.entity.User;
import com.krino.backend.exception.InvalidPasswordResetTokenException;
import com.krino.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenService passwordResetTokenService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Starts a reset. Always succeeds from the caller's point of view whether or not the email
     * maps to an account, so the endpoint can't be used to discover which emails are registered.
     * A link is emailed only when a matching user exists.
     */
    @Transactional
    public void requestReset(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresentOrElse(user -> {
            String rawToken = passwordResetTokenService.issueToken(user);
            String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
            emailService.sendPasswordReset(user.getEmail(), resetLink);
            log.info("Password reset link issued for user {}", user.getId());
        }, () -> log.info("Password reset requested for an unknown email; ignoring."));
    }

    /**
     * Completes a reset: validates the single-use, unexpired token, sets the new password and
     * revokes every existing session so an old or leaked session can't outlive the reset. The
     * new/confirm match is enforced by {@code ResetPasswordRequestDTO} validation.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = passwordResetTokenService.consume(rawToken)
                .orElseThrow(() -> new InvalidPasswordResetTokenException("This password reset link is invalid or has expired."));

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        refreshTokenService.revokeAllTokensForUser(user.getId());
        log.info("Password reset completed for user {}", user.getId());
    }
}
