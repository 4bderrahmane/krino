package com.krino.backend.controller;

import com.krino.backend.entity.User;
import com.krino.backend.entity.UserRole;
import com.krino.backend.repository.RefreshTokenRepository;
import com.krino.backend.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;
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
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class AuthenticationControllerIntegrationTest {
    private static final String RAW_PASSWORD = "Password123!";
    private static final String CANDIDATE_EMAIL = "candidate@test.local";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginWithApprovedUserSetsHttpOnlyAuthCookies() throws Exception {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);

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
    void loginWithUppercaseEmailAuthenticatesNormalizedAccount() throws Exception {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);

        MvcResult result = login(CANDIDATE_EMAIL.toUpperCase(), RAW_PASSWORD)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"email\":\"" + CANDIDATE_EMAIL + "\"");
        assertThat(result.getResponse().getCookie("access_token")).isNotNull();
        assertThat(result.getResponse().getCookie("refresh_token")).isNotNull();
    }

    @Test
    void loginWithUnapprovedUserReturnsUnauthorizedWithoutCookies() throws Exception {
        createUser(CANDIDATE_EMAIL, false, UserRole.CANDIDATE);

        MvcResult result = login(CANDIDATE_EMAIL, RAW_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"errorCode\":\"INVALID_CREDENTIALS\"");
        assertThat(setCookieHeaders(result)).isEmpty();
    }

    @Test
    void registerNormalizesEmailAndCreatesCandidate() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": " Test ",
                                  "lastName": " User ",
                                  "email": "CANDIDATE@TEST.LOCAL",
                                  "password": "%s",
                                  "phoneNumber": "123456789"
                                }
                                """.formatted(RAW_PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"email\":\"" + CANDIDATE_EMAIL + "\"");
        User savedUser = userRepository.findByEmail(CANDIDATE_EMAIL).orElseThrow();
        assertThat(savedUser.getRoles()).contains(UserRole.CANDIDATE);
        assertThat(savedUser.isApproved()).isFalse();
    }

    @Test
    void loginWithInvalidPasswordReturnsUnauthorizedWithoutCookies() throws Exception {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);

        MvcResult result = login(CANDIDATE_EMAIL, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"errorCode\":\"INVALID_CREDENTIALS\"",
                "\"detail\":\"Invalid email or password\""
        );
        assertThat(setCookieHeaders(result)).isEmpty();
    }

    @Test
    void protectedEndpointWithoutAccessCookieReturnsUnauthorized() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"errorCode\":\"UNAUTHORIZED\"",
                "\"instance\":\"/api/users/me\""
        );
    }

    @Test
    void accessTokenCookieAuthenticatesMeEndpoint() throws Exception {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetCookie(CANDIDATE_EMAIL, RAW_PASSWORD, "access_token");

        MvcResult result = mockMvc.perform(get("/api/users/me").cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"email\":\"" + CANDIDATE_EMAIL + "\"",
                "\"firstName\":\"Test\""
        );
    }

    @Test
    void candidateAccessTokenCannotReadAllUsers() throws Exception {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetCookie(CANDIDATE_EMAIL, RAW_PASSWORD, "access_token");

        MvcResult result = mockMvc.perform(get("/api/users").cookie(accessCookie))
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"errorCode\":\"ACCESS_DENIED\"",
                "\"instance\":\"/api/users\""
        );
    }

    @Test
    void refreshRotatesRefreshTokenAndRejectsReusedToken() throws Exception {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        MvcResult loginResult = login(CANDIDATE_EMAIL, RAW_PASSWORD)
                .andExpect(status().isOk())
                .andReturn();
        Cookie originalRefreshCookie = cookie(loginResult, "refresh_token");

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(originalRefreshCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(refreshResult.getResponse().getContentAsString()).contains("\"tokenType\":\"Bearer\"");
        assertThat(setCookieHeaders(refreshResult)).anySatisfy(cookie -> assertThat(cookie).startsWith("access_token" +
                "="));
        assertThat(setCookieHeaders(refreshResult)).anySatisfy(cookie -> assertThat(cookie).startsWith("refresh_token" +
                "="));

        mockMvc.perform(post("/api/auth/refresh").cookie(originalRefreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieReturnsProblemDetail() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"errorCode\":\"UNAUTHORIZED\"",
                "\"detail\":\"No refresh token provided\"",
                "\"instance\":\"/api/auth/refresh\""
        );
    }

    @Test
    void logoutRevokesRefreshTokenAndClearsAuthenticationCookies() throws Exception {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        MvcResult loginResult = login(CANDIDATE_EMAIL, RAW_PASSWORD)
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = cookie(loginResult, "refresh_token");
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(refreshTokenRepository.findAll()).allSatisfy(token -> assertThat(token.isRevoked()).isTrue());
        assertThat(setCookieHeaders(logoutResult)).anySatisfy(cookie -> assertThat(cookie)
                .startsWith("access_token=")
                .contains("Max-Age=0"));
        assertThat(setCookieHeaders(logoutResult)).anySatisfy(cookie -> assertThat(cookie)
                .startsWith("refresh_token=")
                .contains("Max-Age=0"));
    }

    private User createUser(String email, boolean approved, UserRole... roles) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .firstName("Test")
                .lastName("User")
                .phoneNumber("123456789")
                .isApproved(approved)
                .roles(Set.copyOf(Arrays.asList(roles)))
                .build();

        return userRepository.save(user);
    }

    private ResultActions login(String email, String password) throws Exception {
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

    private Cookie loginAndGetCookie(String email, String password, String cookieName) throws Exception {
        MvcResult loginResult = login(email, password)
                .andExpect(status().isOk())
                .andReturn();

        return cookie(loginResult, cookieName);
    }

    private List<String> setCookieHeaders(MvcResult result) {
        return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
    }

    private Cookie cookie(MvcResult result, String cookieName) {
        Cookie cookie = result.getResponse().getCookie(cookieName);
        assertThat(cookie).as("cookie %s", cookieName).isNotNull();
        return cookie;
    }
}
