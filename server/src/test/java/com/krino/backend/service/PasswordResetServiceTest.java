package com.krino.backend.service;

import com.krino.backend.entity.PasswordResetToken;
import com.krino.backend.entity.User;
import com.krino.backend.exception.InvalidPasswordResetTokenException;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.service.email.EmailDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private static final String FRONTEND_URL = "http://localhost:5000";

    private UserRepository userRepository;
    private PasswordResetTokenService passwordResetTokenService;
    private RefreshTokenService refreshTokenService;
    private EmailDispatcher emailDispatcher;
    private PasswordEncoder passwordEncoder;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordResetTokenService = mock(PasswordResetTokenService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        emailDispatcher = mock(EmailDispatcher.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new PasswordResetService(userRepository, passwordResetTokenService, refreshTokenService,
                emailDispatcher, passwordEncoder);
        ReflectionTestUtils.setField(service, "frontendUrl", FRONTEND_URL);
    }

    @Test
    void requestReset_existingUser_emailsTokenizedLink() {
        User user = User.builder().id(1L).email("staff@krino.com").build();
        when(userRepository.findByEmail("staff@krino.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenService.issueToken(user)).thenReturn("RAW-TOKEN");

        // Mixed case + padding on input to confirm normalization before lookup.
        service.requestReset("  Staff@Krino.com ");

        verify(emailDispatcher).sendPasswordReset("staff@krino.com",
                FRONTEND_URL + "/reset-password?token=RAW-TOKEN");
    }

    @Test
    void requestReset_unknownEmail_doesNothing() {
        when(userRepository.findByEmail("ghost@krino.com")).thenReturn(Optional.empty());

        service.requestReset("ghost@krino.com");

        // No enumeration signal: no token issued, no email sent.
        verifyNoInteractions(passwordResetTokenService, emailDispatcher);
    }

    @Test
    void resetPassword_validToken_setsPasswordClearsFlagAndRevokesSessions() {
        User user = User.builder().id(7L).email("staff@krino.com").mustChangePassword(true).build();
        PasswordResetToken token = PasswordResetToken.builder().user(user).build();
        when(passwordResetTokenService.consume("RAW-TOKEN")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("New-Password-1")).thenReturn("encoded");

        service.resetPassword("RAW-TOKEN", "New-Password-1");

        assertThat(user.getPassword()).isEqualTo("encoded");
        assertThat(user.isMustChangePassword()).isFalse();
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllTokensForUser(7L);
    }

    @Test
    void resetPassword_invalidOrExpiredToken_throwsAndChangesNothing() {
        when(passwordResetTokenService.consume("BAD-TOKEN")).thenReturn(Optional.empty());

        assertThrows(InvalidPasswordResetTokenException.class,
                () -> service.resetPassword("BAD-TOKEN", "New-Password-1"));

        verify(userRepository, never()).save(any(User.class));
        verify(refreshTokenService, never()).revokeAllTokensForUser(anyLong());
    }
}
