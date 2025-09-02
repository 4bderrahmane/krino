package com.jesa.interviewslotmanager.service;
//
//import com.InterviewManager.interview_slot_manager.DTO.Authentication.AuthenticationResponseDTO;
//import com.InterviewManager.interview_slot_manager.entity.CustomUserDetails;
//import com.InterviewManager.interview_slot_manager.entity.User;
//import com.InterviewManager.interview_slot_manager.utility.CookieUtilities;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.ResponseCookie;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//import java.util.Optional;
//
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class CookieAuthenticationService
//{
//
//    private final JwtService jwtService;
//    private final RefreshTokenService refreshTokenService;
//
//    @Value("${app.security.cookie.secure:false}")
//    private boolean SECURE_COOKIES;
//
//    @Value("${app.security.cookie.same-site:Strict}")
//    private String SAME_SITE_POLICY;
//
////    public AuthenticationResponseDTO loginWithCookies(CustomUserDetails userDetails, HttpServletResponse response, HttpServletRequest request)
////    {
////        String accessToken = jwtService.generateAccessToken(userDetails);
////
////        String deviceInfo = extractDeviceInfo(request);
////        String ipAddress = extractIpAddress(request);
////
////        User user = userDetails.getUser();
////        String refreshToken = refreshTokenService.generateAndSaveRefreshToken(user, deviceInfo, ipAddress);
////
////        setAccessTokenCookie(response, accessToken);
////        // Remove any legacy refresh_token cookie set on the root path to prevent duplicates
////        clearLegacyRefreshTokenOnRootPath(response);
////        setRefreshTokenCookie(response, refreshToken);
////
////        log.info("Authentication cookies set for user: {}", userDetails.getEmail());
////
////        return AuthenticationResponseDTO.builder()
////                .accessToken(accessToken)
////                .refreshToken(refreshToken)
////                .tokenType("Bearer")
////                .expiresIn(jwtService
////                        .getAccessTokenExpiryInSeconds())
////                .build();
////    }
//
//    public Optional<AuthenticationResponseDTO> refreshFromCookies(HttpServletRequest request, HttpServletResponse response)
//    {
//        return getRefreshTokenFromCookie(request).flatMap(refreshToken ->
//        {
//            if (refreshTokenService.validateRefreshToken(refreshToken))
//            {
//                Optional<User> user = refreshTokenService.getUserFromRefreshToken(refreshToken);
//
//                if (user.isPresent())
//                {
//                    CustomUserDetails userDetails = new CustomUserDetails(user.get());
//                    String newAccessToken = jwtService.generateAccessToken(userDetails);
//
//                    refreshTokenService.updateLastUsed(refreshToken);
//
//                    setAccessTokenCookie(response, newAccessToken);
//
//                    log.debug("Access token refreshed via cookie");
//
//                    return Optional.of(AuthenticationResponseDTO
//                            .builder()
//                            .accessToken(newAccessToken)
//                            .refreshToken(refreshToken)
//                            .tokenType("Bearer")
//                            .build());
//                }
//            }
//            return Optional.empty();
//        });
//    }
//
//    public Optional<String> getAccessTokenFromCookies(HttpServletRequest request)
//    {
//        return getAccessTokenFromCookie(request);
//    }
//
//    public void logoutWithCookies(HttpServletRequest request, HttpServletResponse response)
//    {
//        getRefreshTokenFromCookie(request).ifPresent(refreshToken ->
//        {
//            refreshTokenService.findValidRefreshToken(refreshToken)
//                    .ifPresent(refreshTokenService::revokeToken);
//        });
//
//        clearAuthenticationCookies(response);
//
//        log.info("Authentication cookies cleared for logout");
//    }
//
//    private void setAccessTokenCookie(HttpServletResponse response, String accessToken)
//    {
//        ResponseCookie cookie = ResponseCookie
//                .from("access_token", accessToken)
//                .httpOnly(true)
//                .secure(SECURE_COOKIES)
//                .sameSite(SAME_SITE_POLICY)
//                .path("/")
//                .maxAge(Duration.ofSeconds(jwtService.getAccessTokenExpiryInSeconds()))
//                .build();
//
//        response.addHeader("Set-Cookie", cookie.toString());
//    }
//
////    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken)
////    {
////        ResponseCookie cookie = ResponseCookie
////                .from("refresh_token", refreshToken)
////                .httpOnly(true)
////                .secure(SECURE_COOKIES)
////                .sameSite(SAME_SITE_POLICY)
////                .path("/api/auth")
////                .maxAge(Duration.ofDays(30))
////                .build();
////
////        response.addHeader("Set-Cookie", cookie.toString());
////    }
//
//    // Deletes any legacy refresh_token cookie that might have been set on the root path ("/")
//    // to avoid two refresh_token cookies coexisting with different paths.
//    private void clearLegacyRefreshTokenOnRootPath(HttpServletResponse response)
//    {
//        ResponseCookie legacyRefreshCookieRoot = ResponseCookie.from("refresh_token", "")
//                .httpOnly(true)
//                .secure(SECURE_COOKIES)
//                .sameSite(SAME_SITE_POLICY)
//                .path("/")
//                .maxAge(0)
//                .build();
//
//        response.addHeader("Set-Cookie", legacyRefreshCookieRoot.toString());
//    }
//
//    private Optional<String> getAccessTokenFromCookie(HttpServletRequest request)
//    {
//        if (request.getCookies() != null)
//        {
//            for (jakarta.servlet.http.Cookie cookie : request.getCookies())
//            {
//                if ("access_token".equals(cookie.getName()))
//                {
//                    return Optional.ofNullable(cookie.getValue()).filter(value -> !value.isEmpty());
//                }
//            }
//        }
//        return Optional.empty();
//    }
//
//    private Optional<String> getRefreshTokenFromCookie(HttpServletRequest request)
//    {
//        if (request.getCookies() != null)
//        {
//            for (jakarta.servlet.http.Cookie cookie : request.getCookies())
//            {
//                if ("refresh_token".equals(cookie.getName()))
//                {
//                    return Optional.ofNullable(cookie.getValue()).filter(value -> !value.isEmpty());
//                }
//            }
//        }
//        return Optional.empty();
//    }
//
//    private void clearAuthenticationCookies(HttpServletResponse response)
//    {
//        ResponseCookie accessCookie = ResponseCookie
//                .from("access_token", "")
//                .httpOnly(true)
//                .secure(SECURE_COOKIES)
//                .sameSite(SAME_SITE_POLICY)
//                .path("/")
//                .maxAge(0)
//                .build();
//
//        ResponseCookie refreshCookieAuthPath = ResponseCookie
//                .from("refresh_token", "")
//                .httpOnly(true)
//                .secure(SECURE_COOKIES)
//                .sameSite(SAME_SITE_POLICY)
//                .path("/api/auth")
//                .maxAge(0)
//                .build();
//
//        // Also clear any legacy refresh_token cookie that was set on the root path
//        ResponseCookie refreshCookieRootPath = ResponseCookie
//                .from("refresh_token", "")
//                .httpOnly(true)
//                .secure(SECURE_COOKIES)
//                .sameSite(SAME_SITE_POLICY)
//                .path("/")
//                .maxAge(0)
//                .build();
//
//        response.addHeader("Set-Cookie", accessCookie.toString());
//        response.addHeader("Set-Cookie", refreshCookieAuthPath.toString());
//        response.addHeader("Set-Cookie", refreshCookieRootPath.toString());
//    }
//
//}