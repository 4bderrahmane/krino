package com.krino.backend.controller;

import com.krino.backend.entity.User;
import com.krino.backend.entity.UserRole;
import com.krino.backend.repository.RefreshTokenRepository;
import com.krino.backend.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthenticationControllerIntegrationTest
{
    private static final String RAW_PASSWORD = "Password123!";
    private static final String CANDIDATE_EMAIL = "candidate@test.local";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginWithApprovedUserSetsHttpOnlyAuthCookies() throws Exception
    {
        createUser(CANDIDATE_EMAIL, "candidate", true, UserRole.CANDIDATE);

        MvcResult result = login(CANDIDATE_EMAIL, RAW_PASSWORD)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"tokenType\":\"Bearer\"",
                "\"email\":\"" + CANDIDATE_EMAIL + "\""
        );
        assertThat(setCookieHeaders(result)).anySatisfy(cookie -> assertThat(cookie)
                .startsWith("access_token=")
                .contains("HttpOnly")
                .contains("SameSite=Strict"));
        assertThat(setCookieHeaders(result)).anySatisfy(cookie -> assertThat(cookie)
                .startsWith("refresh_token=")
                .contains("HttpOnly")
                .contains("SameSite=Strict"));
    }

    @Test
    void loginWithInvalidPasswordReturnsUnauthorizedWithoutCookies() throws Exception
    {
        createUser(CANDIDATE_EMAIL, "candidate", true, UserRole.CANDIDATE);

        MvcResult result = login(CANDIDATE_EMAIL, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"errorCode\":\"INVALID_CREDENTIALS\"",
                "\"message\":\"Invalid email or password\""
        );
        assertThat(setCookieHeaders(result)).isEmpty();
    }

    @Test
    void protectedEndpointWithoutAccessCookieReturnsUnauthorized() throws Exception
    {
        MvcResult result = mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"errorCode\":\"AUTHENTICATION_REQUIRED\"",
                "\"path\":\"/api/users/me\""
        );
    }

    @Test
    void accessTokenCookieAuthenticatesMeEndpoint() throws Exception
    {
        createUser(CANDIDATE_EMAIL, "candidate", true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetCookie(CANDIDATE_EMAIL, RAW_PASSWORD, "access_token");

        MvcResult result = mockMvc.perform(get("/api/users/me").cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"email\":\"" + CANDIDATE_EMAIL + "\"",
                "\"username\":\"candidate\""
        );
    }

    @Test
    void candidateAccessTokenCannotReadAllUsers() throws Exception
    {
        createUser(CANDIDATE_EMAIL, "candidate", true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetCookie(CANDIDATE_EMAIL, RAW_PASSWORD, "access_token");

        MvcResult result = mockMvc.perform(get("/api/users").cookie(accessCookie))
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"errorCode\":\"ACCESS_DENIED\"",
                "\"path\":\"/api/users\""
        );
    }

    @Test
    void refreshRotatesRefreshTokenAndRejectsReusedToken() throws Exception
    {
        createUser(CANDIDATE_EMAIL, "candidate", true, UserRole.CANDIDATE);
        MvcResult loginResult = login(CANDIDATE_EMAIL, RAW_PASSWORD)
                .andExpect(status().isOk())
                .andReturn();
        Cookie originalRefreshCookie = cookie(loginResult, "refresh_token");

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(originalRefreshCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(refreshResult.getResponse().getContentAsString()).contains("\"tokenType\":\"Bearer\"");
        assertThat(setCookieHeaders(refreshResult)).anySatisfy(cookie -> assertThat(cookie).startsWith("access_token="));
        assertThat(setCookieHeaders(refreshResult)).anySatisfy(cookie -> assertThat(cookie).startsWith("refresh_token="));

        mockMvc.perform(post("/api/auth/refresh").cookie(originalRefreshCookie))
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String email, String username, boolean approved, UserRole... roles)
    {
        User user = User.builder()
                .email(email)
                .username(username)
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .firstName("Test")
                .lastName("User")
                .phoneNumber("123456789")
                .isApproved(approved)
                .roles(Set.copyOf(Arrays.asList(roles)))
                .build();

        return userRepository.save(user);
    }

    private ResultActions login(String email, String password) throws Exception
    {
        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Cookie loginAndGetCookie(String email, String password, String cookieName) throws Exception
    {
        MvcResult loginResult = login(email, password)
                .andExpect(status().isOk())
                .andReturn();

        return cookie(loginResult, cookieName);
    }

    private List<String> setCookieHeaders(MvcResult result)
    {
        return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
    }

    private Cookie cookie(MvcResult result, String cookieName)
    {
        Cookie cookie = result.getResponse().getCookie(cookieName);
        assertThat(cookie).as("cookie %s", cookieName).isNotNull();
        return cookie;
    }
}
