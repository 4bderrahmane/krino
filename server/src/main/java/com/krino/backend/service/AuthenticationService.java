package com.krino.backend.service;

import com.krino.backend.dto.authentication.AuthenticationResponseDTO;
import com.krino.backend.dto.user.UserLoginDTO;
import com.krino.backend.dto.user.UserRegistrationDTO;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.entity.User;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.entity.RefreshToken;
import com.krino.backend.exception.AccountNotApprovedException;
import com.krino.backend.exception.BaseException;
import com.krino.backend.exception.EmailNotVerifiedException;
import com.krino.backend.exception.InvalidCredentialsException;
import com.krino.backend.exception.InvalidRefreshTokenException;
import com.krino.backend.exception.PasswordChangeRequiredException;
import com.krino.backend.mapper.UserMapper;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.service.email.EmailVerificationService;
import com.krino.backend.service.resume.ResumeStorageService;
import com.krino.backend.service.resume.StoredResume;
import com.krino.backend.utility.CookieUtilities;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final CookieUtilities cookieUtilities;
    private final ResumeStorageService resumeStorageService;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public void register(@NonNull final UserRegistrationDTO dto, @NonNull final MultipartFile resume) {

        String email = normalizeEmail(dto.getEmail());

        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Registration ignored for already-registered email");
            return;
        }

        User user = userMapper.toEntity(dto, email, passwordEncoder.encode(dto.getPassword()));
        user.addRole(UserRole.CANDIDATE);
        User savedUser = userRepository.save(user);
        StoredResume storedResume = resumeStorageService.uploadUserResume(savedUser.getPublicId(), resume);
        applyResume(savedUser, storedResume);
        savedUser.setApproved(true);

        // The account exists but cannot log in until the emailed verification link is used.
        emailVerificationService.sendVerificationEmail(savedUser);

        log.info("User registered successfully with email: {}", email);
    }

    private void applyResume(User user, StoredResume resume) {
        user.setResumeObjectKey(resume.objectKey());
        user.setResumeOriginalFilename(resume.originalFilename());
        user.setResumeContentType(resume.contentType());
        user.setResumeSizeBytes(resume.sizeBytes());
        user.setResumeUploadedAt(resume.uploadedAt());
    }

    @Transactional
    public AuthenticationResponseDTO login(@NonNull final UserLoginDTO dto, HttpServletResponse response, HttpServletRequest request) {

        String email = normalizeEmail(dto.getEmail());
        String password = dto.getPassword();

        authenticateUser(email, password);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(String.format("User not found with email: %s",
                        email)));


        assertAccountMayHoldSession(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UserResponseDTO userResponse = userMapper.toResponse(user);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.generateAndSaveRefreshToken(user, extractDeviceInfo(request),
                extractIpAddress(request));

        cookieUtilities.setCookies(accessToken, refreshToken, response);

        return new AuthenticationResponseDTO("Bearer", jwtService.getAccessTokenExpiryInSeconds(), userResponse);
    }

    private void authenticateUser(String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            if (!authentication.isAuthenticated()) {
                log.warn("Authentication was not successful for email: {}", email);
                throw new InvalidCredentialsException("Authentication failed");
            }

        } catch (DisabledException _) {
            log.warn("Authentication blocked: account deactivated for email: {}", email);
            throw new AccountNotApprovedException("Your account has been deactivated. Please contact an administrator" +
                    ".");
        } catch (AuthenticationException _) {
            log.warn("Authentication failed for email: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public AuthenticationResponseDTO refresh(HttpServletRequest request, HttpServletResponse response) {
        String providedRefreshToken = cookieUtilities.getRefreshTokenFromCookie(request)
                .orElseThrow(() -> new InvalidRefreshTokenException("No refresh token provided"));


        // Account state is settled before the row lock below is taken, and deliberately so.
        // Rejecting revokes the whole token family in its own transaction, and that UPDATE
        // would touch the very row this transaction had locked FOR UPDATE: the inner
        // transaction would wait on a lock only the outer one can release, and the outer is
        // suspended waiting for the inner. That is a hang with no timeout, not an error.
        // An invalid or unknown token yields no user here and falls through to the normal
        // rejection path below.
        refreshTokenService.getUserFromRefreshToken(providedRefreshToken)
                .ifPresent(user -> assertSessionMayBeRenewed(user, response));

        RefreshToken token = refreshTokenService.findValidRefreshTokenForUpdate(providedRefreshToken)
                .orElseThrow(() -> rejectInvalidRefreshToken(providedRefreshToken, request));

        User user = token.getUser();
        if (user == null)
            throw new InvalidRefreshTokenException("User not found for refresh token");

        refreshTokenService.consumeToken(token);
        String newRefreshToken = refreshTokenService.generateAndSaveRefreshToken(
                user,
                extractDeviceInfo(request),
                extractIpAddress(request)
        );


        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = jwtService.generateAccessToken(userDetails);


        cookieUtilities.setAccessTokenCookie(response, newAccessToken);
        cookieUtilities.setRefreshTokenCookie(response, newRefreshToken);

        UserResponseDTO userResponse = userMapper.toResponse(user);

        log.info("Access and refresh tokens rotated successfully for user: {}", user.getEmail());

        return AuthenticationResponseDTO.builder()
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiryInSeconds())
                .user(userResponse)
                .build();
    }

    /**
     * The account conditions a session depends on, checked wherever one is granted.
     *
     * <p>Approval is normally enforced before this by the {@link AuthenticationManager}, which
     * reads it through {@link CustomUserDetails#isEnabled()}; it is restated here because
     * {@link #refresh} has no authentication manager in its path and must not be allowed to
     * drift from the login rules.
     */
    private void assertAccountMayHoldSession(User user) {
        if (!user.isApproved()) {
            log.warn("Session refused: account deactivated for user {}", user.getId());
            throw new AccountNotApprovedException("Your account has been deactivated. Please contact an administrator.");
        }

        if (!user.isEmailVerified()) {
            log.warn("Session refused: email not verified for user {}", user.getId());
            throw new EmailNotVerifiedException("Please verify your email address before signing in.");
        }
    }

    /**
     * Renewal re-asks every question login asked, plus one of its own.
     *
     * <p>A refresh token is a 30-day licence to mint access tokens, and nothing else revisits
     * the account's state while it lasts. Without this, deactivating or unverifying an account
     * anywhere other than {@code UserService#setApproval}, which deletes tokens as a side
     * effect, would leave its live sessions renewable until the token aged out. The extra
     * question is the temporary password: login allows it so the user can reach the
     * change-password endpoint, but renewing would turn "change this now" into "never".
     *
     * <p>Rejection revokes the whole family rather than merely refusing this call, because an
     * account that cannot hold a session should not be left holding usable refresh tokens. The
     * revocation runs in its own transaction, since throwing here rolls this one back. That is
     * also why the caller runs this before locking the presented token: see {@link #refresh}.
     */
    private void assertSessionMayBeRenewed(User user, HttpServletResponse response) {
        try {
            assertAccountMayHoldSession(user);

            if (user.isMustChangePassword()) {
                log.warn("Refresh refused: password change still pending for user {}", user.getId());
                throw new PasswordChangeRequiredException(
                        "Please set a new password before continuing; sign in again to do so.");
            }
        } catch (BaseException rejection) {
            refreshTokenService.revokeAllSessionsInOwnTransaction(user.getId());
            cookieUtilities.clearAuthenticationCookies(response);
            throw rejection;
        }
    }

    /**
     * A refresh token that exists but was already consumed is the signature of replay after
     * rotation: either the legitimate client or a thief now holds the successor, and there is
     * no way to tell which party is asking. The whole session family is therefore revoked
     * (in its own transaction, so it survives this request's rollback) and everyone has to
     * log in again. The response stays identical to the token-never-existed case so a
     * probing attacker can't distinguish the two.
     */
    private InvalidRefreshTokenException rejectInvalidRefreshToken(String providedRefreshToken, HttpServletRequest request) {
        refreshTokenService.findRefreshTokenAnyState(providedRefreshToken)
                .filter(token -> token.isConsumed() && token.getUser() != null)
                .ifPresent(replayed -> {
                    Long userId = replayed.getUser().getId();
                    log.warn("SECURITY: refresh token reuse detected for user {} (ip: {}, device: {}); revoking all"
                            + " sessions", userId, extractIpAddress(request), extractDeviceInfo(request));
                    refreshTokenService.revokeAllSessionsInOwnTransaction(userId);
                });

        return new InvalidRefreshTokenException("Invalid or expired refresh token");
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        cookieUtilities.getRefreshTokenFromCookie(request)
                .flatMap(refreshTokenService::findValidRefreshToken)
                .ifPresent(refreshTokenService::revokeToken);

        cookieUtilities.clearAuthenticationCookies(response);

        log.info("User logged out successfully");
    }

    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown Device";
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) return xForwardedFor.split(",")[0].trim();

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) return xRealIp;

        return request.getRemoteAddr();
    }

}
