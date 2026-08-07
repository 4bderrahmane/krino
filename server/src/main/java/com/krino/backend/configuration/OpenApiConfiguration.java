package com.krino.backend.configuration;

import com.krino.backend.configuration.properties.AuthenticationProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    private static final String ACCESS_TOKEN_COOKIE = "accessTokenCookie";

    @Bean
    public OpenAPI krinoOpenApi(AuthenticationProperties authenticationProperties) {
        String accessCookieName = authenticationProperties.accessCookieName();

        return new OpenAPI()
                .info(new Info()
                        .title("Krino API")
                        .description("""
                                REST API for Krino, a system for scheduling and managing candidate \
                                interviews.
                                
                                Authentication uses an HttpOnly `%s` JWT cookie issued by \
                                `POST /api/auth/login` and renewed via `POST /api/auth/refresh`. \
                                State-changing requests must also send the CSRF token from the \
                                `XSRF-TOKEN` cookie in the `X-XSRF-TOKEN` header (Swagger UI does \
                                this automatically).""".formatted(accessCookieName))
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(ACCESS_TOKEN_COOKIE, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(accessCookieName)
                                .description("JWT access token cookie set by POST /api/auth/login")))
                // Applies to every documented operation. Controllers reachable anonymously opt out
                // with an empty @SecurityRequirements; this describes the API, it does not guard
                // it — access is decided by SecurityConfiguration alone.
                .addSecurityItem(new SecurityRequirement().addList(ACCESS_TOKEN_COOKIE));
    }
}
