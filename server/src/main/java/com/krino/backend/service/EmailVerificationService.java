package com.krino.backend.service;

import com.krino.backend.entity.EmailVerificationToken;
import com.krino.backend.entity.User;
import com.krino.backend.exception.InvalidEmailVerificationTokenException;
import com.krino.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Issues a fresh verification token (invalidating any earlier ones) and emails the link.
     * Called at registration and from the resend endpoint.
     */
    public void sendVerificationEmail(User user) {
        String rawToken = emailVerificationTokenService.issueToken(user);
        String verificationLink = frontendUrl + "/verify-email?token=" + rawToken;
        emailService.sendEmailVerification(user.getEmail(), user.getFirstName(), verificationLink);
        log.info("Email verification link issued for user {}", user.getId());
    }

    /**
     * Resend flow. Always succeeds from the caller's point of view whether or not the email
     * maps to an account, so the endpoint can't be used to discover which emails are
     * registered. A link is emailed only for an existing, still-unverified user.
     */
    @Transactional
    public void resendVerification(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresentOrElse(user -> {
            if (user.isEmailVerified()) {
                log.info("Verification resend requested for already-verified user {}; ignoring.", user.getId());
                return;
            }
            sendVerificationEmail(user);
        }, () -> log.info("Verification resend requested for an unknown email; ignoring."));
    }

    /**
     * Completes verification: validates the single-use, unexpired token and marks the owning
     * account's email as verified, which unblocks login.
     */
    @Transactional
    public void verifyEmail(String rawToken) {
        EmailVerificationToken token = emailVerificationTokenService.consume(rawToken)
                .orElseThrow(() -> new InvalidEmailVerificationTokenException(
                        "This verification link is invalid or has expired."));

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Email verified for user {}", user.getId());
    }
}
