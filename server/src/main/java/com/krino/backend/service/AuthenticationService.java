package com.krino.backend.service;

import com.krino.backend.dto.authentication.AuthenticationResponseDTO;
import com.krino.backend.dto.authentication.RegistrationResponseDTO;
import com.krino.backend.dto.user.UserLoginDTO;
import com.krino.backend.dto.user.UserRegistrationDTO;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.entity.User;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.entity.RefreshToken;
import com.krino.backend.exception.AccountNotApprovedException;
import com.krino.backend.exception.InvalidCredentialsException;
import com.krino.backend.exception.InvalidRefreshTokenException;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.mapper.UserMapper;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.utility.CookieUtilities;
import com.krino.backend.utility.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
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

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private static final String EMAIL_ALREADY_TAKEN_MESSAGE = "Email '%s' is already taken.";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final CookieUtilities cookieUtilities;
    private final CvStorageService cvStorageService;

    @Transactional
    public RegistrationResponseDTO register(@NonNull final UserRegistrationDTO request, @NonNull final MultipartFile resume) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty())
            throw new IllegalArgumentException("Email cannot be null or empty");

        if (request.getPassword() == null || request.getPassword().trim().isEmpty())
            throw new IllegalArgumentException("Password cannot be null or empty");

        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ResourceConflictException(String.format(EMAIL_ALREADY_TAKEN_MESSAGE, normalizedEmail),
                    ErrorCode.DATA_CONFLICT,
                    Map.of("field", "email", "value", normalizedEmail));
        }

        User user = userMapper.toEntity(request, normalizedEmail, passwordEncoder.encode(request.getPassword()));
        user.addRole(UserRole.CANDIDATE);

        User savedUser = userRepository.save(user);

        CvStorageService.StoredResume storedResume = cvStorageService.uploadUserResume(savedUser.getPublicId(), resume);
        applyResume(savedUser, storedResume);
        savedUser.setApproved(true);

        UserResponseDTO userResponse = userMapper.toResponse(savedUser);

        log.info("User registered successfully with email: {}", normalizedEmail);
        return new RegistrationResponseDTO(userResponse, "User registered successfully.");
    }

    private void applyResume(User user, CvStorageService.StoredResume resume) {
        user.setResumeObjectKey(resume.objectKey());
        user.setResumeOriginalFilename(resume.originalFilename());
        user.setResumeContentType(resume.contentType());
        user.setResumeSizeBytes(resume.sizeBytes());
        user.setResumeUploadedAt(resume.uploadedAt());
    }

    @Transactional
    public AuthenticationResponseDTO login(@NonNull final UserLoginDTO request, HttpServletResponse response,
                                           HttpServletRequest httpRequest) {

        String normalizedEmail = normalizeEmail(request.getEmail());

        authenticateUser(normalizedEmail, request.getPassword());

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException(String.format("User not found with email: %s",
                        normalizedEmail)));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UserResponseDTO userResponse = userMapper.toResponse(user);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.generateAndSaveRefreshToken(user, extractDeviceInfo(httpRequest),
                extractIpAddress(httpRequest));

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
            throw new AccountNotApprovedException("Your account has been deactivated. Please contact an administrator.");
        } catch (AuthenticationException _) {
            log.warn("Authentication failed for email: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public AuthenticationResponseDTO refresh(HttpServletRequest request, HttpServletResponse response) {
        String providedRefreshToken = cookieUtilities.getRefreshTokenFromCookie(request)
                .orElseThrow(() -> new InvalidRefreshTokenException("No refresh token provided"));


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
     * A refresh token that exists but was already consumed is the signature of replay after
     * rotation: either the legitimate client or a thief now holds the successor, and there is
     * no way to tell which party is asking. The whole session family is therefore revoked
     * (in its own transaction, so it survives this request's rollback) and everyone has to
     * log in again. The response stays identical to the token-never-existed case so a
     * probing attacker can't distinguish the two.
     */
    private InvalidRefreshTokenException rejectInvalidRefreshToken(String providedRefreshToken,
                                                                   HttpServletRequest request) {
        refreshTokenService.findRefreshTokenAnyState(providedRefreshToken)
                .filter(token -> token.isConsumed() && token.getUser() != null)
                .ifPresent(replayed -> {
                    Long userId = replayed.getUser().getId();
                    log.warn("SECURITY: refresh token reuse detected for user {} (ip: {}, device: {}); revoking all"
                            + " sessions", userId, extractIpAddress(request), extractDeviceInfo(request));
                    refreshTokenService.handleCompromisedToken(userId);
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
