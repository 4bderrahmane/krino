package com.krino.backend.utility;

import com.krino.backend.configuration.properties.AuthenticationProperties;
import com.krino.backend.configuration.properties.CookieProperties;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.ResponseCookie;

@Slf4j
@Component
public class CookieUtilities {
    private static final String COOKIE_HEADER_NAME = "Set-Cookie";
    private static final String ACCESS_TOKEN_COOKIE_PATH = "/";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/auth";
    private static final String SAME_SITE = "Strict";

    private final AuthenticationProperties authenticationProperties;
    private final CookieProperties cookieProperties;

    public CookieUtilities(AuthenticationProperties authenticationProperties, CookieProperties cookieProperties) {
        this.authenticationProperties = authenticationProperties;
        this.cookieProperties = cookieProperties;
    }

    @PostConstruct
    void init() {
        if (!cookieProperties.secure()) {
            log.warn("Auth cookies are being issued WITHOUT the Secure flag (app.cookies.secure=false). " +
                    "This should be used only for local development.");
        }
    }

    private ResponseCookie generateAccessCookie(String value) {
        return ResponseCookie
                .from(authenticationProperties.accessCookieName(), value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(SAME_SITE)
                .path(ACCESS_TOKEN_COOKIE_PATH)
                .maxAge(authenticationProperties.accessCookieMaxAge())
                .build();
    }

    private ResponseCookie generateRefreshCookie(String value) {
        return ResponseCookie
                .from(authenticationProperties.refreshCookieName(), value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(SAME_SITE)
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(authenticationProperties.refreshCookieMaxAge())
                .build();
    }

    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        ResponseCookie cookie = generateAccessCookie(accessToken);

        response.addHeader(COOKIE_HEADER_NAME, cookie.toString());
        log.debug("Access token cookie set with max age: {} seconds", authenticationProperties.accessCookieMaxAge().toSeconds());
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = generateRefreshCookie(refreshToken);

        response.addHeader(COOKIE_HEADER_NAME, cookie.toString());
        log.debug("Refresh token cookie set with max age: {} seconds", authenticationProperties.refreshCookieMaxAge().toSeconds());
    }

    public void setCookies(String accessToken, String refreshToken, HttpServletResponse response) {
        setRefreshTokenCookie(response, refreshToken);
        setAccessTokenCookie(response, accessToken);
    }


    public Optional<String> getAccessTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, authenticationProperties.accessCookieName());
    }

    public Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, authenticationProperties.refreshCookieName());
    }

    public void clearAuthenticationCookies(HttpServletResponse response) {
        clearCookie(response, authenticationProperties.accessCookieName(), ACCESS_TOKEN_COOKIE_PATH);
        clearCookie(response, authenticationProperties.refreshCookieName(), REFRESH_TOKEN_COOKIE_PATH);
        log.debug("Authentication cookies cleared");
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> cookieName.equals(cookie.getName()))
                    .findFirst()
                    .map(jakarta.servlet.http.Cookie::getValue)
                    .filter(StringUtils::hasText);
        }
        return Optional.empty();
    }

    private void clearCookie(HttpServletResponse response, String cookieName, String path) {
        ResponseCookie cookie = ResponseCookie
                .from(cookieName, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(SAME_SITE)
                .path(path)
                .maxAge(0)
                .build();

        response.addHeader(COOKIE_HEADER_NAME, cookie.toString());
    }

}
