package com.krino.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.repository.RefreshTokenRepository;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.service.CvStorageService;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    // Registration uploads the candidate's base CV to object storage; stub it so the
    // integration test does not require a running MinIO.
    @MockitoBean
    private CvStorageService cvStorageService;

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
                .contains("Path=/")
                .contains("HttpOnly")
                .contains("SameSite=Strict"));
        assertThat(setCookieHeaders(result)).anySatisfy(cookie -> assertThat(cookie)
                .startsWith("refresh_token=")
                .contains("Path=/api/auth")
                .contains("HttpOnly")
                .contains("SameSite=Strict"));
    }

    @Test
    void csrfEndpointReturnsReadableTokenCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        CsrfTokenResponse csrfToken = objectMapper.readValue(result.getResponse().getContentAsString(),
                CsrfTokenResponse.class);

        assertThat(csrfToken.cookieName()).isEqualTo("XSRF-TOKEN");
        assertThat(csrfToken.headerName()).isEqualTo("X-XSRF-TOKEN");
        assertThat(setCookieHeaders(result)).anySatisfy(cookie -> assertThat(cookie)
                .startsWith("XSRF-TOKEN=")
                .contains("Path=/")
                .doesNotContain("HttpOnly"));
    }

    @Test
    void loginWithCsrfCookieAndHeaderSucceeds() throws Exception {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        CsrfExchange csrf = fetchCsrfToken();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(CANDIDATE_EMAIL, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getCookie("access_token")).isNotNull();
        assertThat(result.getResponse().getCookie("refresh_token")).isNotNull();
    }

    @Test
    void mutatingAuthRequestWithoutCsrfTokenReturnsForbidden() throws Exception {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(CANDIDATE_EMAIL, RAW_PASSWORD)))
                .andExpect(status().isForbidden());
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
        when(cvStorageService.uploadUserResume(any(), any())).thenReturn(
                new CvStorageService.StoredResume("users/key/resume/cv.pdf", "cv.pdf", "application/pdf", 1024L,
                        LocalDateTime.of(2026, Month.JANUARY, 15, 10, 30)));

        MockMultipartFile data = new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, """
                {
                  "firstName": " Test ",
                  "lastName": " User ",
                  "email": "CANDIDATE@TEST.LOCAL",
                  "password": "%s",
                  "phoneNumber": "123456789"
                }
                """.formatted(RAW_PASSWORD).getBytes());
        MockMultipartFile resume = new MockMultipartFile("resume", "cv.pdf", "application/pdf",
                "%PDF-1.7\ncontent".getBytes());

        MvcResult result = mockMvc.perform(withCsrf(multipart("/api/auth/register").file(data).file(resume)))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"email\":\"" + CANDIDATE_EMAIL + "\"",
                "\"resumeFilename\":\"cv.pdf\"");
        User savedUser = userRepository.findByEmail(CANDIDATE_EMAIL).orElseThrow();
        assertThat(savedUser.getRoles()).contains(UserRole.CANDIDATE);
        assertThat(savedUser.isApproved()).isFalse();
        assertThat(savedUser.getResumeObjectKey()).isEqualTo("users/key/resume/cv.pdf");
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

        MvcResult refreshResult = mockMvc.perform(withCsrf(post("/api/auth/refresh"))
                        .cookie(originalRefreshCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(refreshResult.getResponse().getContentAsString()).contains("\"tokenType\":\"Bearer\"");
        assertThat(setCookieHeaders(refreshResult)).anySatisfy(cookie -> assertThat(cookie)
                .startsWith("access_token=")
                .contains("Path=/"));
        assertThat(setCookieHeaders(refreshResult)).anySatisfy(cookie -> assertThat(cookie)
                .startsWith("refresh_token=")
                .contains("Path=/api/auth"));

        mockMvc.perform(withCsrf(post("/api/auth/refresh"))
                        .cookie(originalRefreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieReturnsProblemDetail() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(post("/api/auth/refresh")))
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

        MvcResult logoutResult = mockMvc.perform(withCsrf(post("/api/auth/logout"))
                        .cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(refreshTokenRepository.findAll()).allSatisfy(token -> assertThat(token.isRevoked()).isTrue());
        assertThat(setCookieHeaders(logoutResult)).anySatisfy(cookie -> assertThat(cookie)
                .startsWith("access_token=")
                .contains("Path=/")
                .contains("Max-Age=0"));
        assertThat(setCookieHeaders(logoutResult)).anySatisfy(cookie -> assertThat(cookie)
                .startsWith("refresh_token=")
                .contains("Path=/api/auth")
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

        return mockMvc.perform(withCsrf(post("/api/auth/login"))
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

    private <B extends AbstractMockHttpServletRequestBuilder<B>> B withCsrf(B request) throws Exception {
        CsrfExchange csrf = fetchCsrfToken();
        return request.cookie(csrf.cookie())
                .header(csrf.headerName(), csrf.token());
    }

    private CsrfExchange fetchCsrfToken() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        CsrfTokenResponse csrfToken = objectMapper.readValue(csrfResult.getResponse().getContentAsString(),
                CsrfTokenResponse.class);
        Cookie xsrfCookie = cookie(csrfResult, csrfToken.cookieName());
        return new CsrfExchange(csrfToken.headerName(), xsrfCookie.getValue(), xsrfCookie);
    }

    private record CsrfTokenResponse(String cookieName, String headerName) {
    }

    private record CsrfExchange(String headerName, String token, Cookie cookie) {
    }
}
