package com.krino.backend.security;

import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.service.CustomUserDetailsService;
import com.krino.backend.service.JwtService;
import com.krino.backend.utility.CookieUtilities;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtCookieAuthenticationFilterTest {
    private static final String ACCESS_TOKEN = "access-token";
    private static final UUID PUBLIC_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final JwtService jwtService = mock(JwtService.class);
    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final CookieUtilities cookieUtilities = mock(CookieUtilities.class);
    private final CustomAuthenticationEntryPoint authenticationEntryPoint =
            mock(CustomAuthenticationEntryPoint.class);
    private final JwtCookieAuthenticationFilter filter = new JwtCookieAuthenticationFilter(
            jwtService,
            userDetailsService,
            cookieUtilities,
            authenticationEntryPoint
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validCookieForDisabledUserIsRejectedAsAuthenticationFailure() throws Exception {
        MockHttpServletRequest request = protectedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        stubVerifiedToken(request);
        when(userDetailsService.loadUserByPublicId(PUBLIC_ID)).thenReturn(userDetails);
        when(userDetails.isAccountNonLocked()).thenReturn(true);
        when(userDetails.isEnabled()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetails, never()).getAuthorities();
        verify(authenticationEntryPoint).commence(eq(request), eq(response), any(DisabledException.class));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void validCookieForEnabledUserCreatesAuthenticationAndContinuesChain() throws Exception {
        MockHttpServletRequest request = protectedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        CustomUserDetails userDetails = enabledUserDetails();
        stubVerifiedToken(request);
        when(userDetailsService.loadUserByPublicId(PUBLIC_ID)).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull()
                .extracting("principal")
                .isSameAs(userDetails);
        verify(filterChain).doFilter(request, response);
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
    }

    @Test
    void infrastructureFailureIsNotSwallowedAsAuthenticationFailure() throws Exception {
        MockHttpServletRequest request = protectedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        DataAccessResourceFailureException databaseFailure =
                new DataAccessResourceFailureException("database unavailable");
        stubVerifiedToken(request);
        when(userDetailsService.loadUserByPublicId(PUBLIC_ID)).thenThrow(databaseFailure);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isSameAs(databaseFailure);

        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void authEndpointIsSkippedAndDownstreamExceptionPropagatesOnce() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        ServletException downstreamFailure = new ServletException("controller failed");
        doThrow(downstreamFailure).when(filterChain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isSameAs(downstreamFailure);

        verify(filterChain).doFilter(request, response);
        verify(cookieUtilities, never()).getAccessTokenFromCookie(request);
    }

    private MockHttpServletRequest protectedRequest() {
        return new MockHttpServletRequest("GET", "/api/users/me");
    }

    private void stubVerifiedToken(MockHttpServletRequest request) {
        when(cookieUtilities.getAccessTokenFromCookie(request)).thenReturn(Optional.of(ACCESS_TOKEN));
        when(jwtService.parseAccessToken(ACCESS_TOKEN))
                .thenReturn(new JwtService.VerifiedAccessToken(PUBLIC_ID, Instant.now().plusSeconds(900)));
    }

    private CustomUserDetails enabledUserDetails() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.isAccountNonLocked()).thenReturn(true);
        when(userDetails.isEnabled()).thenReturn(true);
        when(userDetails.isAccountNonExpired()).thenReturn(true);
        when(userDetails.isCredentialsNonExpired()).thenReturn(true);
        when(userDetails.getAuthorities()).thenReturn(List.of());
        return userDetails;
    }
}
