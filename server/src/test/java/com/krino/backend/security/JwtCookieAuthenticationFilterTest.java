package com.krino.backend.security;

import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.service.CustomUserDetailsService;
import com.krino.backend.service.JwtService;
import com.krino.backend.utility.CookieUtilities;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtCookieAuthenticationFilterTest {
    private final JwtService jwtService = mock(JwtService.class);
    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final CookieUtilities cookieUtilities = mock(CookieUtilities.class);
    private final JwtCookieAuthenticationFilter filter = new JwtCookieAuthenticationFilter(jwtService, userDetailsService, cookieUtilities);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validCookieForDisabledUserDoesNotAuthenticateRequest() throws Exception {
        String accessToken = "access-token";
        UUID publicId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(cookieUtilities.getAccessTokenFromCookie(request)).thenReturn(Optional.of(accessToken));
        when(jwtService.validateToken(accessToken)).thenReturn(true);
        when(jwtService.getUserPublicIdFromToken(accessToken)).thenReturn(publicId);
        when(userDetailsService.loadUserByPublicId(publicId)).thenReturn(userDetails);
        when(userDetails.isEnabled()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetails, never()).getAuthorities();
        verify(filterChain).doFilter(request, response);
    }
}
