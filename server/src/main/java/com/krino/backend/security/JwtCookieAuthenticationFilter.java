package com.krino.backend.security;

import com.krino.backend.service.CustomUserDetailsService;
import com.krino.backend.service.JwtService;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.utility.CookieUtilities;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final CookieUtilities cookieUtilities;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestPath = request.getRequestURI();
            if (requestPath.startsWith("/api/auth/")) {
                filterChain.doFilter(request, response);
                return;
            }

            cookieUtilities.getAccessTokenFromCookie(request).ifPresent(accessToken ->
            {
                try {
                    if (jwtService.validateToken(accessToken)) {
                        CustomUserDetails userDetails =
                                customUserDetailsService.loadUserByPublicId(jwtService.getUserPublicIdFromToken(accessToken));

                        if (!userDetails.isEnabled()) {
                            log.debug("Cookie authentication rejected for disabled user: {}", userDetails.getEmail());
                            SecurityContextHolder.clearContext();
                            return;
                        }

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                        userDetails.getAuthorities());

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        log.debug("User authenticated via cookie: {}", userDetails.getEmail());
                    }
                } catch (Exception e) {
                    log.debug("Cookie authentication failed: {}", e.getMessage());
                    SecurityContextHolder.clearContext();
                }
            });

        } catch (Exception e) {
            log.error("Error in JWT cookie authentication filter: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
