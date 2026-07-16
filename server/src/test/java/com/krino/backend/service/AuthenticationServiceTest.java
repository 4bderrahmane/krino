package com.krino.backend.service;

import com.krino.backend.dto.user.UserLoginDTO;
import com.krino.backend.dto.user.UserRegistrationDTO;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.exception.EmailNotVerifiedException;
import com.krino.backend.exception.InvalidRefreshTokenException;
import com.krino.backend.mapper.UserMapper;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.service.email.EmailVerificationService;
import com.krino.backend.utility.CookieUtilities;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest
{
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private RefreshTokenService refreshTokenService;
    private AuthenticationManager authenticationManager;
    private UserMapper userMapper;
    private CookieUtilities cookieUtilities;
    private CvStorageService cvStorageService;
    private EmailVerificationService emailVerificationService;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp()
    {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        authenticationManager = mock(AuthenticationManager.class);
        userMapper = mock(UserMapper.class);
        cookieUtilities = mock(CookieUtilities.class);
        cvStorageService = mock(CvStorageService.class);
        emailVerificationService = mock(EmailVerificationService.class);

        authenticationService = new AuthenticationService(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService,
                authenticationManager,
                userMapper,
                cookieUtilities,
                cvStorageService,
                emailVerificationService
        );
    }

    @Test
    void registerNormalizesEmailBeforeLookupAndPersist()
    {
        UserRegistrationDTO request = new UserRegistrationDTO(
                " Test ",
                " User ",
                "Candidate@TEST.Local",
                "Password123!",
                "123456789"
        );
        when(userRepository.findByEmail("candidate@test.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded");
        when(userMapper.toEntity(request, "candidate@test.local", "encoded")).thenReturn(new User(
                "candidate@test.local",
                "encoded",
                "Test",
                "User",
                "123456789"
        ));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cvStorageService.uploadUserResume(any(), any(MultipartFile.class))).thenReturn(
                new CvStorageService.StoredResume("users/key/resume/cv.pdf", "cv.pdf", "application/pdf", 1024L,
                        LocalDateTime.of(2026, Month.JANUARY, 15, 10, 30)));

        MockMultipartFile resume = new MockMultipartFile(
                "resume", "cv.pdf", "application/pdf", "%PDF-1.7\ncontent".getBytes());

        authenticationService.register(request, resume);

        verify(userRepository).findByEmail("candidate@test.local");
        verify(cvStorageService).uploadUserResume(any(), any(MultipartFile.class));
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getEmail().equals("candidate@test.local")
                        && user.getFirstName().equals("Test")
                        && user.getLastName().equals("User")
                        && user.getRoles().contains(UserRole.CANDIDATE)
        ));
        // Registration must kick off the verification email; the account stays unverified.
        verify(emailVerificationService).sendVerificationEmail(any(User.class));
    }

    @Test
    void registerWithAlreadyUsedEmailIsSilentlyIgnoredToAvoidAccountEnumeration()
    {
        UserRegistrationDTO request = new UserRegistrationDTO(
                "Test",
                "User",
                "candidate@test.local",
                "Password123!",
                "123456789"
        );
        when(userRepository.findByEmail("candidate@test.local"))
                .thenReturn(Optional.of(approvedCandidate()));

        MockMultipartFile resume = new MockMultipartFile(
                "resume", "cv.pdf", "application/pdf", "%PDF-1.7\ncontent".getBytes());

        authenticationService.register(request, resume);

        // Silently ignored: nothing is written and no email goes out for the existing account,
        // and the endpoint's 204 is indistinguishable from a fresh signup.
        verify(userRepository, never()).save(any(User.class));
        verify(cvStorageService, never()).uploadUserResume(any(), any(MultipartFile.class));
        verify(emailVerificationService, never()).sendVerificationEmail(any(User.class));
    }

    @Test
    void loginWithUnverifiedEmailIsRejectedAfterSuccessfulAuthentication()
    {
        User user = approvedCandidate();
        user.setEmailVerified(false);
        Authentication authenticated = new UsernamePasswordAuthenticationToken(
                "candidate@test.local",
                null,
                List.of()
        );
        UserLoginDTO request = new UserLoginDTO("candidate@test.local", "Password123!");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        HttpServletResponse httpResponse = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticated);
        when(userRepository.findByEmail("candidate@test.local")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.login(request, httpResponse, httpRequest))
                .isInstanceOf(EmailNotVerifiedException.class);

        // No session material may be issued for an unverified account.
        verify(refreshTokenService, never()).generateAndSaveRefreshToken(any(), any(), any());
        verify(cookieUtilities, never()).setCookies(any(), any(), any());
    }

    @Test
    void loginNormalizesEmailBeforeAuthenticationAndUserLookup()
    {
        User user = approvedCandidate();
        UserResponseDTO response = new UserResponseDTO();
        response.setEmail("candidate@test.local");
        Authentication authenticated = new UsernamePasswordAuthenticationToken(
                "candidate@test.local",
                null,
                List.of()
        );
        UserLoginDTO request = new UserLoginDTO("Candidate@TEST.Local", "Password123!");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("User-Agent", "JUnit");
        httpRequest.setRemoteAddr("203.0.113.10");
        HttpServletResponse httpResponse = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticated);
        when(userRepository.findByEmail("candidate@test.local")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(refreshTokenService.generateAndSaveRefreshToken(user, "JUnit", "203.0.113.10"))
                .thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpiryInSeconds()).thenReturn(900L);

        authenticationService.login(request, httpResponse, httpRequest);

        verify(authenticationManager).authenticate(org.mockito.ArgumentMatchers.argThat(authentication ->
                authentication instanceof UsernamePasswordAuthenticationToken
                        && authentication.getPrincipal().equals("candidate@test.local")
                        && authentication.getCredentials().equals("Password123!")
        ));
        verify(userRepository).findByEmail("candidate@test.local");
        verify(cookieUtilities).setCookies("access-token", "refresh-token", httpResponse);
    }

    @Test
    void refreshWithoutCookieThrowsInvalidRefreshTokenException()
    {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        assertThatThrownBy(() -> authenticationService.refresh(httpRequest, httpResponse))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("No refresh token provided");
    }

    private static User approvedCandidate()
    {
        User user = new User();
        user.setId(1L);
        user.setPublicId(UUID.randomUUID());
        user.setEmail("candidate@test.local");
        user.setPassword("encoded");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setApproved(true);
        user.setEmailVerified(true);
        user.setRoles(Set.of(UserRole.CANDIDATE));
        return user;
    }
}
