package com.krino.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.repository.RefreshTokenRepository;
import com.krino.backend.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base for full-stack controller tests: boots the application, wires a security-aware
 * {@link MockMvc}, and provides user-creation, login and CSRF helpers. Subclasses clean
 * their own entity tables; refresh tokens (which reference users) are cleared here first.
 */
@SpringBootTest
abstract class AbstractControllerIntegrationTest
{
    protected static final String RAW_PASSWORD = "Password123!";
    protected static final String ADMIN_EMAIL = "admin@test.local";
    protected static final String INTERVIEWER_EMAIL = "interviewer@test.local";
    protected static final String CANDIDATE_EMAIL = "candidate@test.local";

    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected WebApplicationContext webApplicationContext;

    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected MockMvc mockMvc;

    @BeforeEach
    void initMockMvcAndRefreshTokens()
    {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        refreshTokenRepository.deleteAll();
    }

    protected User createUser(String email, boolean approved, UserRole... roles)
    {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .firstName("Test")
                .lastName("User")
                .phoneNumber("123456789")
                .isApproved(approved)
                .roles(Set.of(roles))
                .build();

        return userRepository.save(user);
    }

    protected Cookie loginAndGetAccessCookie(String email) throws Exception
    {
        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, RAW_PASSWORD);

        MvcResult loginResult = mockMvc.perform(withCsrf(post("/api/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = loginResult.getResponse().getCookie("access_token");
        assertThat(cookie).as("access_token cookie").isNotNull();
        return cookie;
    }

    protected MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception
    {
        CsrfExchange csrf = fetchCsrfToken();
        return request.cookie(csrf.cookie())
                .header(csrf.headerName(), csrf.token());
    }

    private CsrfExchange fetchCsrfToken() throws Exception
    {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        CsrfTokenResponse csrfToken = objectMapper.readValue(csrfResult.getResponse().getContentAsString(),
                CsrfTokenResponse.class);
        Cookie xsrfCookie = csrfResult.getResponse().getCookie(csrfToken.cookieName());
        assertThat(xsrfCookie).as("XSRF-TOKEN cookie").isNotNull();
        return new CsrfExchange(csrfToken.headerName(), xsrfCookie.getValue(), xsrfCookie);
    }

    private record CsrfTokenResponse(String cookieName, String headerName) {
    }

    private record CsrfExchange(String headerName, String token, Cookie cookie) {
    }
}
