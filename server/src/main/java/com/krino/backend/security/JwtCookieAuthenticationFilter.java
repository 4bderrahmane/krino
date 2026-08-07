package com.krino.backend.security;

import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.service.CustomUserDetailsService;
import com.krino.backend.service.JwtService;
import com.krino.backend.utility.CookieUtilities;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    // The anonymous catalogue must not care about cookies. Browsers keep sending an expired
    // access_token long after a session lapses, and this filter answers a rejected token with
    // 401 rather than falling through; without this, a returning visitor would be locked out of
    // pages that need no login at all. Skipping the filter means these endpoints always run
    // anonymous, which is exactly what they are specified to do.
    private static final RequestMatcher PUBLIC_ENDPOINTS = PathPatternRequestMatcher.pathPattern("/api/public/**");
    private static final RequestMatcher AUTH_ENDPOINTS = PathPatternRequestMatcher.pathPattern("/api/auth/**");

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final CookieUtilities cookieUtilities;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return AUTH_ENDPOINTS.matches(request) || PUBLIC_ENDPOINTS.matches(request);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        var accessToken = cookieUtilities.getAccessTokenFromCookie(request);
        if (accessToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtService.VerifiedAccessToken verifiedToken = jwtService.parseAccessToken(accessToken.get());
            CustomUserDetails userDetails = customUserDetailsService.loadUserByPublicId(verifiedToken.userPublicId());

            userDetailsChecker.check(userDetails);

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            log.debug("User authenticated via access-token cookie: {}", userDetails.getEmail());
        } catch (AuthenticationException authenticationFailure) {
            SecurityContextHolder.clearContext();
            log.debug("Cookie authentication rejected: {}", authenticationFailure.getMessage());
            authenticationEntryPoint.commence(request, response, authenticationFailure);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
