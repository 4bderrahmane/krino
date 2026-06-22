package com.krino.backend.service;

import com.krino.backend.dto.authentication.RegistrationResponseDTO;
import com.krino.backend.dto.user.UserLoginDTO;
import com.krino.backend.dto.user.UserRegistrationDTO;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.exception.InvalidRefreshTokenException;
import com.krino.backend.mapper.UserMapper;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.utility.CookieUtilities;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

        authenticationService = new AuthenticationService(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService,
                authenticationManager,
                userMapper,
                cookieUtilities
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
        UserResponseDTO response = new UserResponseDTO();
        response.setEmail("candidate@test.local");

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
        when(userMapper.toResponse(any(User.class))).thenReturn(response);

        RegistrationResponseDTO registration = authenticationService.register(request);

        assertThat(registration.getUser().getEmail()).isEqualTo("candidate@test.local");
        verify(userRepository).findByEmail("candidate@test.local");
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getEmail().equals("candidate@test.local")
                        && user.getFirstName().equals("Test")
                        && user.getLastName().equals("User")
                        && user.getRoles().contains(UserRole.CANDIDATE)
        ));
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
        verify(cookieUtilities).setCookies("access-token", "refresh-token", httpResponse, "/", "/api/auth/");
    }

    @Test
    void refreshWithoutCookieThrowsInvalidRefreshTokenException()
    {
        assertThatThrownBy(() -> authenticationService.refresh(new MockHttpServletRequest(), new MockHttpServletResponse()))
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
        user.setRoles(Set.of(UserRole.CANDIDATE));
        return user;
    }
}
