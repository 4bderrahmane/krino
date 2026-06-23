package com.krino.backend.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigurationTest
{

    @Test
    void corsConfigurationAllowsConfiguredClientOriginForLoginPreflight()
    {
        SecurityConfiguration securityConfiguration = new SecurityConfiguration(null, null, null, null, null);
        CorsConfigurationSource source = securityConfiguration.corsConfigurationSource(List.of(
                "http://localhost:5000",
                "http://localhost:5173"
        ));

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
        CorsConfiguration corsConfiguration = source.getCorsConfiguration(request);

        assertNotNull(corsConfiguration);
        assertEquals("http://localhost:5000", corsConfiguration.checkOrigin("http://localhost:5000"));
        assertTrue(corsConfiguration.checkHttpMethod(HttpMethod.POST).contains(HttpMethod.POST));
        assertTrue(corsConfiguration.checkHttpMethod(HttpMethod.OPTIONS).contains(HttpMethod.OPTIONS));
        assertTrue(corsConfiguration.checkHeaders(List.of("Content-Type", "X-XSRF-TOKEN")).contains("X-XSRF-TOKEN"));
        assertNull(corsConfiguration.checkOrigin("http://localhost:3000"));
    }
}
