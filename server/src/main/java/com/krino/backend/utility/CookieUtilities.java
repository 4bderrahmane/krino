package com.krino.backend.utility;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.ResponseCookie;

@Slf4j
@Component
public class CookieUtilities {
    private static final String COOKIE_HEADER_NAME = "Set-Cookie";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String ACCESS_TOKEN_COOKIE_PATH = "/";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/auth";
    private static final Duration ACCESS_TOKEN_COOKIE_MAX_AGE = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_COOKIE_MAX_AGE = Duration.ofDays(30);
    private static final String SAME_SITE = "Strict";

    private final boolean cookieSecure;

    public CookieUtilities(@Value("${app.cookies.secure:true}") boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    @PostConstruct
    void init() {
        if (!cookieSecure)
            log.warn("Auth cookies are being issued WITHOUT the Secure flag (app.cookies.secure=false). Use this only" +
                    " for local HTTP development.");
    }

    private ResponseCookie generateAccessCookie(String value) {
        return ResponseCookie
                .from(ACCESS_TOKEN_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(SAME_SITE)
                .path(ACCESS_TOKEN_COOKIE_PATH)
                .maxAge(ACCESS_TOKEN_COOKIE_MAX_AGE)
                .build();
    }

    private ResponseCookie generateRefreshCookie(String value) {
        return ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(SAME_SITE)
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(REFRESH_TOKEN_COOKIE_MAX_AGE)
                .build();
    }

    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        ResponseCookie cookie = generateAccessCookie(accessToken);

        response.addHeader(COOKIE_HEADER_NAME, cookie.toString());
        log.debug("Access token cookie set with max age: {} seconds", ACCESS_TOKEN_COOKIE_MAX_AGE.getSeconds());
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {

        ResponseCookie cookie = generateRefreshCookie(refreshToken);

        response.addHeader(COOKIE_HEADER_NAME, cookie.toString());
        log.debug("Refresh token cookie set with max age: {} days", REFRESH_TOKEN_COOKIE_MAX_AGE.toDays());
    }

    public void setCookies(String accessToken, String refreshToken, HttpServletResponse response) {
        setRefreshTokenCookie(response, refreshToken);
        setAccessTokenCookie(response, accessToken);
    }


    public Optional<String> getAccessTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
    }

    public Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);
    }

    public void clearAuthenticationCookies(HttpServletResponse response) {
        clearCookie(response, ACCESS_TOKEN_COOKIE_NAME, ACCESS_TOKEN_COOKIE_PATH);
        clearCookie(response, REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN_COOKIE_PATH);
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
                .secure(cookieSecure)
                .sameSite(SAME_SITE)
                .path(path)
                .maxAge(0)
                .build();

        response.addHeader(COOKIE_HEADER_NAME, cookie.toString());
    }

}
