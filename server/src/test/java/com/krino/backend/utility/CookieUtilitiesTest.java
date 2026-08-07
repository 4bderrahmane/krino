package com.krino.backend.utility;

import com.krino.backend.configuration.properties.AuthenticationProperties;
import com.krino.backend.configuration.properties.CookieProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilitiesTest {
    private static final Duration ACCESS_TTL = Duration.ofMinutes(17);
    private static final Duration REFRESH_TTL = Duration.ofDays(21);
    private static final String ACCESS_COOKIE = "custom_access";
    private static final String REFRESH_COOKIE = "custom_refresh";
    private static final AuthenticationProperties AUTHENTICATION_PROPERTIES = new AuthenticationProperties(
            "krino-test",
            "0123456789abcdef0123456789abcdef",
            ACCESS_TTL,
            REFRESH_TTL,
            ACCESS_COOKIE,
            REFRESH_COOKIE
    );

    private final CookieUtilities cookieUtilities = new CookieUtilities(
            AUTHENTICATION_PROPERTIES,
            new CookieProperties(false)
    );

    @Test
    void issuedCookiesUseConfiguredNamesAndMatchingTokenLifetimes() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtilities.setCookies("access-value", "refresh-value", response);

        assertThat(response.getHeaders("Set-Cookie"))
                .anySatisfy(header -> assertThat(header)
                        .startsWith(ACCESS_COOKIE + "=access-value")
                        .contains("Path=/")
                        .contains("Max-Age=" + ACCESS_TTL.toSeconds()))
                .anySatisfy(header -> assertThat(header)
                        .startsWith(REFRESH_COOKIE + "=refresh-value")
                        .contains("Path=/api/auth")
                        .contains("Max-Age=" + REFRESH_TTL.toSeconds()))
                .allSatisfy(header -> assertThat(header).doesNotContain("Secure"));
    }

    @Test
    void issuedCookiesHonorSecureConfiguration() {
        CookieUtilities secureCookieUtilities = new CookieUtilities(
                AUTHENTICATION_PROPERTIES,
                new CookieProperties(true)
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        secureCookieUtilities.setCookies("access-value", "refresh-value", response);

        assertThat(response.getHeaders("Set-Cookie"))
                .allSatisfy(header -> assertThat(header).contains("Secure"));
    }

    @Test
    void cookieLookupAndClearingUseConfiguredNames() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(ACCESS_COOKIE, "access-value"),
                new Cookie(REFRESH_COOKIE, "refresh-value")
        );

        assertThat(cookieUtilities.getAccessTokenFromCookie(request)).contains("access-value");
        assertThat(cookieUtilities.getRefreshTokenFromCookie(request)).contains("refresh-value");

        MockHttpServletResponse response = new MockHttpServletResponse();
        cookieUtilities.clearAuthenticationCookies(response);

        assertThat(response.getHeaders("Set-Cookie"))
                .anySatisfy(header -> assertThat(header)
                        .startsWith(ACCESS_COOKIE + "=")
                        .contains("Path=/")
                        .contains("Max-Age=0"))
                .anySatisfy(header -> assertThat(header)
                        .startsWith(REFRESH_COOKIE + "=")
                        .contains("Path=/api/auth")
                        .contains("Max-Age=0"));
    }
}
