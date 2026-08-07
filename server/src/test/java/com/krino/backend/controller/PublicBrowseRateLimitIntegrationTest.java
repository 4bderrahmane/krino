package com.krino.backend.controller;

import com.krino.backend.entity.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The anonymous catalogue has no login to slow a scraper down, so the public-browse bucket is
 * the only thing bounding it. The two budgets are deliberately far apart here (2 vs 50) so a
 * mix-up between them cannot pass.
 */
@TestPropertySource(properties = {
        "app.ratelimit.enabled=true",
        "app.ratelimit.capacity=50",
        "app.ratelimit.refill-period=1h",
        "app.ratelimit.idle-expiry=2h",
        "app.ratelimit.public-browse.capacity=2",
        "app.ratelimit.public-browse.refill-period=1h"
})
class PublicBrowseRateLimitIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void throttlesAnonymousBrowsingOnItsOwnBudget() throws Exception {
        browse()
                .andExpect(status().isOk())
                .andExpect(header().string("RateLimit-Limit", "2"));
        browse().andExpect(status().isOk());

        browse()
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"));
    }

    @Test
    void browsingDoesNotSpendTheAuthenticationBudget() throws Exception {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);

        // Drain the public bucket completely.
        browse();
        browse();
        browse().andExpect(status().isTooManyRequests());

        // Logging in draws on the separate, larger auth budget, so it still succeeds. If the
        // two tiers shared a Redis bucket, this would come back throttled.
        loginAndGetAccessCookie(CANDIDATE_EMAIL);
    }

    private ResultActions browse() throws Exception {
        return mockMvc.perform(get("/api/public/jobs"));
    }
}
