package com.jesa.interviewslotmanager.utility;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
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
import org.springframework.web.util.WebUtils;

@Slf4j
@Component
public class CookieUtilities
{
    private static final String COOKIE_HEADER_NAME = "Set-Cookie";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    // A cookie can only be deleted by replaying the same path it was set with,
    // so these must match the paths used when the cookies are issued.
    private static final String ACCESS_TOKEN_COOKIE_PATH = "/";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/auth/";
    private static final Duration ACCESS_TOKEN_COOKIE_MAX_AGE = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_COOKIE_MAX_AGE = Duration.ofDays(30);
    private static final String SAME_SITE = "Strict";

    // Whether to set the Secure flag (cookies only sent over HTTPS). Defaults to true;
    // set app.cookies.secure=false for local plain-HTTP development only.
    private static boolean cookieSecure = true;

    @Value("${app.cookies.secure:true}")
    private boolean cookieSecureProperty;

    @PostConstruct
    void init()
    {
        cookieSecure = cookieSecureProperty;
        if (!cookieSecure)
        {
            log.warn("Auth cookies are being issued WITHOUT the Secure flag (app.cookies.secure=false). Use this only for local HTTP development.");
        }
    }

    private static ResponseCookie generateAccessCookie(String name, String value, String path)
    {
        return ResponseCookie
                .from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(SAME_SITE)
                .path(path)
                .maxAge(ACCESS_TOKEN_COOKIE_MAX_AGE)
                .build();
    }

    private static ResponseCookie generateRefreshCookie(String name, String value, String path)
    {
        return ResponseCookie
                .from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(SAME_SITE)
                .path(path)
                .maxAge(REFRESH_TOKEN_COOKIE_MAX_AGE)
                .build();
    }

    public static void setAccessTokenCookie(HttpServletResponse response, String accessToken, String path)
    {
        ResponseCookie cookie = generateAccessCookie(ACCESS_TOKEN_COOKIE_NAME, accessToken, path);

        response.addHeader(COOKIE_HEADER_NAME, cookie.toString());
        log.debug("Access token cookie set with max age: {} seconds", ACCESS_TOKEN_COOKIE_MAX_AGE.getSeconds());
    }

    public static void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, String path)
    {

        ResponseCookie cookie = generateRefreshCookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken, path);

        response.addHeader(COOKIE_HEADER_NAME, cookie.toString());
        log.debug("Refresh token cookie set with max age: {} days", REFRESH_TOKEN_COOKIE_MAX_AGE.toDays());
    }

    public void setCookies(String accessToken, String refreshToken, HttpServletResponse response, String accessTokenPath, String refreshTokenPath)
    {
        setRefreshTokenCookie(response, refreshToken, refreshTokenPath);
        setAccessTokenCookie(response, accessToken, accessTokenPath);
    }


    public static Optional<String> getAccessTokenFromCookie(HttpServletRequest request)
    {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
    }

    public static Optional<String> getRefreshTokenFromCookie(HttpServletRequest request)
    {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);
    }

    public static void clearAuthenticationCookies(HttpServletResponse response)
    {
        clearCookie(response, ACCESS_TOKEN_COOKIE_NAME, ACCESS_TOKEN_COOKIE_PATH);
        clearCookie(response, REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN_COOKIE_PATH);
        log.debug("Authentication cookies cleared");
    }

    private static Optional<String> getCookieValue(HttpServletRequest request, String cookieName)
    {
        if (request.getCookies() != null)
        {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> cookieName.equals(cookie.getName()))
                    .findFirst()
                    .map(jakarta.servlet.http.Cookie::getValue)
                    .filter(StringUtils::hasText);
        }
        return Optional.empty();
    }

    public static void clearCookie(HttpServletResponse response, String cookieName, String path)
    {
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


    public String getCookieValueByName(HttpServletRequest request, String name)
    {
        Cookie cookie = WebUtils.getCookie(request, name);
        if (cookie != null)
        {
            return cookie.getValue();
        } else
        {
            return null;
        }
    }
}
