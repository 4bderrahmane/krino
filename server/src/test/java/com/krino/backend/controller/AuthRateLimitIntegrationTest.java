package com.krino.backend.controller;

import com.krino.backend.entity.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.ratelimit.enabled=true",
        "app.ratelimit.capacity=2",
        "app.ratelimit.refill-period=1h",
        "app.ratelimit.idle-expiry=2h"
})
class AuthRateLimitIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void throttlesAuthEndpointOncePerIpCapacityIsExceeded() throws Exception {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(CANDIDATE_EMAIL, RAW_PASSWORD);

        // capacity = 2 -> the first two logins from this IP pass, the third is throttled.
        login(body).andExpect(status().isOk());
        login(body).andExpect(status().isOk());

        login(body)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"));
    }

    private ResultActions login(String body) throws Exception {
        return mockMvc.perform(withCsrf(post("/api/auth/login"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
