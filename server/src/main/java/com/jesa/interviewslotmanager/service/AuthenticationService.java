package com.jesa.interviewslotmanager.service;

import com.jesa.interviewslotmanager.dto.Authentication.AuthenticationResponseDTO;
import com.jesa.interviewslotmanager.dto.Authentication.RegistrationResponseDTO;
import com.jesa.interviewslotmanager.dto.User.UserLoginDTO;
import com.jesa.interviewslotmanager.dto.User.UserRegistrationDTO;
import com.jesa.interviewslotmanager.dto.User.UserResponseDTO;
import com.jesa.interviewslotmanager.entity.User;
import com.jesa.interviewslotmanager.entity.CustomUserDetails;
import com.jesa.interviewslotmanager.entity.UserRole;
import com.jesa.interviewslotmanager.entity.RefreshToken;
import com.jesa.interviewslotmanager.exception.EmailAlreadyExistsException;
import com.jesa.interviewslotmanager.exception.InvalidCredentialsException;
import com.jesa.interviewslotmanager.exception.InvalidRefreshTokenException;
import com.jesa.interviewslotmanager.exception.UsernameAlreadyExistsException;
import com.jesa.interviewslotmanager.repository.UserRepository;
import com.jesa.interviewslotmanager.utility.CookieUtilities;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService
{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final ModelMapper modelMapper;
    private final CookieUtilities cookieUtilities;

    @Transactional
    public RegistrationResponseDTO register(@NonNull final UserRegistrationDTO request)
    {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty())
        {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty())
        {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty())
        {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedUsername = request.getUsername().trim().toLowerCase();

        if (userRepository.findByUsername(normalizedUsername).isPresent())
        {
            throw new UsernameAlreadyExistsException("An account with this username already exists: " + normalizedUsername);
        }

        if (userRepository.findByEmail(normalizedEmail).isPresent())
        {
            throw new EmailAlreadyExistsException("An account with this email already exists: " + normalizedEmail);
        }

        try
        {
            User user = new User(
                    normalizedEmail,
                    normalizedUsername,
                    passwordEncoder.encode(request.getPassword()),
                    request.getFirstName() != null ? request.getFirstName().trim() : "",
                    request.getLastName() != null ? request.getLastName().trim() : "",
                    request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null
            );

            user.addRole(UserRole.CANDIDATE);

            User savedUser = userRepository.save(user);

            UserResponseDTO userResponse = modelMapper.map(savedUser, UserResponseDTO.class);

            log.info("User registered successfully with email: {}", normalizedEmail);
            return new RegistrationResponseDTO(userResponse, "User registered successfully.");

        } catch (Exception e)
        {
            log.error("Error during user registration for email {}: {}", normalizedEmail, e.getMessage());
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    @Transactional
    public AuthenticationResponseDTO login(@NonNull final UserLoginDTO request, HttpServletResponse response, HttpServletRequest httpRequest)
    {

        authenticateUser(request.getEmail(), request.getPassword());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + request.getEmail()));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UserResponseDTO userResponse = modelMapper.map(user, UserResponseDTO.class);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.generateAndSaveRefreshToken(user, extractDeviceInfo(httpRequest), extractIpAddress(httpRequest));

        cookieUtilities.setCookies(accessToken, refreshToken, response, "/", "/api/auth/");

        AuthenticationResponseDTO authenticationResponseDto = new AuthenticationResponseDTO(accessToken, refreshToken, "Bearer", 360L, userResponse);

        return authenticationResponseDto;
    }

    private String extractDeviceInfo(HttpServletRequest request)
    {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown Device";
    }

    private String extractIpAddress(HttpServletRequest request)
    {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty())
        {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty())
        {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private Authentication authenticateUser(String email, String password)
    {
        try
        {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            if (!authentication.isAuthenticated())
            {
                log.warn("Authentication was not successful for email: {}", email);
                throw new InvalidCredentialsException("Authentication failed");
            }

            return authentication;
        } catch (AuthenticationException e)
        {
            log.warn("Authentication failed for email: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public AuthenticationResponseDTO refresh(HttpServletRequest request, HttpServletResponse response)
    {
        String providedRefreshToken = cookieUtilities.getCookieValueByName(request, "refresh_token");
        if (providedRefreshToken == null)
        {
            throw new InvalidRefreshTokenException("No refresh token provided");
        }


        RefreshToken tokenEntity = refreshTokenService.findValidRefreshToken(providedRefreshToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid or expired refresh token"));

        User user = tokenEntity.getUser();
        if (user == null)
        {
            throw new InvalidRefreshTokenException("User not found for refresh token");
        }

        refreshTokenService.revokeToken(tokenEntity);
        String newRefreshToken = refreshTokenService.generateAndSaveRefreshToken(
                user,
                extractDeviceInfo(request),
                extractIpAddress(request)
        );


        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = jwtService.generateAccessToken(userDetails);


        CookieUtilities.setAccessTokenCookie(response, newAccessToken, "/");
        CookieUtilities.setRefreshTokenCookie(response, newRefreshToken, "/api/auth/");

        UserResponseDTO userResponse = modelMapper.map(user, UserResponseDTO.class);

        log.info("Access and refresh tokens rotated successfully for user: {}", user.getEmail());

        return AuthenticationResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiryInSeconds())
                .user(userResponse)
                .build();
    }

    public String logout(HttpServletRequest request, HttpServletResponse response)
    {
        CookieUtilities.getRefreshTokenFromCookie(request)
                .ifPresent(refreshToken ->
                        refreshTokenService.findValidRefreshToken(refreshToken)
                                .ifPresent(refreshTokenService::revokeToken)
                );

        CookieUtilities.clearAuthenticationCookies(response);

        log.info("User logged out successfully");
        return "User logged out successfully";
    }
}