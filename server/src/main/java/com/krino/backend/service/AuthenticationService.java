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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    @Transactional
    public RegistrationResponseDTO register(@NonNull final UserRegistrationDTO request) {
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

        UserResponseDTO userResponse = userMapper.toResponse(savedUser);

        log.info("User registered successfully with email: {}", normalizedEmail);
        return new RegistrationResponseDTO(userResponse, "User registered successfully.");
    }

    @Transactional
    public AuthenticationResponseDTO login(@NonNull final UserLoginDTO request, HttpServletResponse response, HttpServletRequest httpRequest) {

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

        cookieUtilities.setCookies(accessToken, refreshToken, response, "/", "/api/auth/");

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

        } catch (AuthenticationException e) {
            log.warn("Authentication failed for email: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public AuthenticationResponseDTO refresh(HttpServletRequest request, HttpServletResponse response) {
        String providedRefreshToken = cookieUtilities.getCookieValueByName(request, "refresh_token");
        if (providedRefreshToken == null)
            throw new InvalidRefreshTokenException("No refresh token provided");


        RefreshToken tokenEntity = refreshTokenService.findValidRefreshTokenForUpdate(providedRefreshToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid or expired refresh token"));

        User user = tokenEntity.getUser();
        if (user == null)
            throw new InvalidRefreshTokenException("User not found for refresh token");

        refreshTokenService.consumeToken(tokenEntity);
        String newRefreshToken = refreshTokenService.generateAndSaveRefreshToken(
                user,
                extractDeviceInfo(request),
                extractIpAddress(request)
        );


        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = jwtService.generateAccessToken(userDetails);


        CookieUtilities.setAccessTokenCookie(response, newAccessToken, "/");
        CookieUtilities.setRefreshTokenCookie(response, newRefreshToken, "/api/auth/");

        UserResponseDTO userResponse = userMapper.toResponse(user);

        log.info("Access and refresh tokens rotated successfully for user: {}", user.getEmail());

        return AuthenticationResponseDTO.builder()
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiryInSeconds())
                .user(userResponse)
                .build();
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtilities.getRefreshTokenFromCookie(request)
                .flatMap(refreshTokenService::findValidRefreshToken)
                .ifPresent(refreshTokenService::revokeToken);

        CookieUtilities.clearAuthenticationCookies(response);

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
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

}
