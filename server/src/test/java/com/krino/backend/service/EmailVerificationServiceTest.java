package com.krino.backend.service;

import com.krino.backend.entity.EmailVerificationToken;
import com.krino.backend.entity.User;
import com.krino.backend.exception.InvalidEmailVerificationTokenException;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.service.email.EmailDispatcher;
import com.krino.backend.service.email.EmailVerificationService;
import com.krino.backend.service.email.EmailVerificationTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest
{
    private static final String FRONTEND_URL = "http://localhost:5000";

    private UserRepository userRepository;
    private EmailVerificationTokenService emailVerificationTokenService;
    private EmailDispatcher emailDispatcher;
    private EmailVerificationService service;

    @BeforeEach
    void setUp()
    {
        userRepository = mock(UserRepository.class);
        emailVerificationTokenService = mock(EmailVerificationTokenService.class);
        emailDispatcher = mock(EmailDispatcher.class);
        service = new EmailVerificationService(userRepository, emailVerificationTokenService, emailDispatcher);
        ReflectionTestUtils.setField(service, "frontendUrl", FRONTEND_URL);
    }

    @Test
    void sendVerificationEmailBuildsFrontendLinkFromRawToken()
    {
        User user = user(false);
        when(emailVerificationTokenService.issueToken(user)).thenReturn("raw-token");

        service.sendVerificationEmail(user);

        verify(emailDispatcher).sendEmailVerification(
                eq("candidate@test.local"),
                eq("Test"),
                eq(FRONTEND_URL + "/verify-email?token=raw-token"));
    }

    @Test
    void resendVerificationIgnoresUnknownEmailWithoutFailing()
    {
        when(userRepository.findByEmail("nobody@test.local")).thenReturn(Optional.empty());

        service.resendVerification(" Nobody@TEST.Local ");

        verify(emailVerificationTokenService, never()).issueToken(any());
        verify(emailDispatcher, never()).sendEmailVerification(any(), any(), any());
    }

    @Test
    void resendVerificationIgnoresAlreadyVerifiedUser()
    {
        User user = user(true);
        when(userRepository.findByEmail("candidate@test.local")).thenReturn(Optional.of(user));

        service.resendVerification("candidate@test.local");

        verify(emailVerificationTokenService, never()).issueToken(any());
        verify(emailDispatcher, never()).sendEmailVerification(any(), any(), any());
    }

    @Test
    void resendVerificationIssuesFreshLinkForUnverifiedUser()
    {
        User user = user(false);
        when(userRepository.findByEmail("candidate@test.local")).thenReturn(Optional.of(user));
        when(emailVerificationTokenService.issueToken(user)).thenReturn("fresh-token");

        service.resendVerification("candidate@test.local");

        verify(emailDispatcher).sendEmailVerification(eq("candidate@test.local"), eq("Test"),
                contains("token=fresh-token"));
    }

    @Test
    void verifyEmailMarksUserVerifiedAndPersists()
    {
        User user = user(false);
        EmailVerificationToken token = EmailVerificationToken.builder().user(user).build();
        when(emailVerificationTokenService.consume("raw-token")).thenReturn(Optional.of(token));

        service.verifyEmail("raw-token");

        assertThat(user.isEmailVerified()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmailWithInvalidTokenThrowsWithoutTouchingUsers()
    {
        when(emailVerificationTokenService.consume("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyEmail("bad-token"))
                .isInstanceOf(InvalidEmailVerificationTokenException.class);

        verify(userRepository, never()).save(any());
    }

    private static User user(boolean verified)
    {
        User user = new User();
        user.setId(7L);
        user.setEmail("candidate@test.local");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmailVerified(verified);
        return user;
    }
}
