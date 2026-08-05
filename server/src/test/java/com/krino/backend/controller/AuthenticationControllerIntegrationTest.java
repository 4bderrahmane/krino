package com.krino.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krino.backend.configuration.properties.StorageProperties;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.repository.ApplicationRepository;
import com.krino.backend.repository.EmailVerificationTokenRepository;
import com.krino.backend.repository.InterviewRepository;
import com.krino.backend.repository.RefreshTokenRepository;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.service.email.EmailService;
import com.krino.backend.support.AbstractIntegrationTest;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.mockito.ArgumentCaptor;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class AuthenticationControllerIntegrationTest extends AbstractIntegrationTest {
    private static final String RAW_PASSWORD = "Password123!";
    private static final String CANDIDATE_EMAIL = "candidate@test.local";
    private static final byte[] PDF_BYTES = "%PDF-1.7\ncontent".getBytes();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final SlotRepository slotRepository;
    private final PasswordEncoder passwordEncoder;
    private final WebApplicationContext webApplicationContext;
    private final MinioClient minioClient;
    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    // Mocked so tests can capture the verification link (and with it the raw token)
    // instead of sending real email through Resend.
    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        // Each class wipes the tables it uses, so rows from the previous class are still
        // around and interviews/applications/slots all point at users. Clear them first,
        // otherwise deleting the users below trips their foreign keys.
        interviewRepository.deleteAll();
        applicationRepository.deleteAll();
        slotRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginWithApprovedUserSetsHttpOnlyAuthCookies() throws Exception {
        createUser(true);

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
        createUser(true);
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
        createUser(true);

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
        createUser(true);

        MvcResult result = login(CANDIDATE_EMAIL.toUpperCase(), RAW_PASSWORD)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"email\":\"" + CANDIDATE_EMAIL + "\"");
        assertThat(result.getResponse().getCookie("access_token")).isNotNull();
        assertThat(result.getResponse().getCookie("refresh_token")).isNotNull();
    }

    @Test
    void loginWithUnapprovedUserReturnsForbiddenWithoutCookies() throws Exception {
        createUser(false);

        MvcResult result = login(CANDIDATE_EMAIL, RAW_PASSWORD)
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"errorCode\":\"ACCOUNT_NOT_APPROVED\"");
        assertThat(setCookieHeaders(result)).isEmpty();
    }

    @Test
    void registerNormalizesEmailAndCreatesCandidate() throws Exception {
        MockMultipartFile data = new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, """
                {
                  "firstName": " Test ",
                  "lastName": " User ",
                  "email": "CANDIDATE@TEST.LOCAL",
                  "password": "%s",
                  "phoneNumber": "123456789"
                }
                """.formatted(RAW_PASSWORD).getBytes());
        MockMultipartFile resume = new MockMultipartFile("resume", "cv.pdf", "application/pdf", PDF_BYTES);

        MvcResult result = mockMvc.perform(withCsrf(multipart("/api/auth/register").file(data).file(resume)))
                .andExpect(status().isNoContent())
                .andReturn();

        // Empty body: the server emits no account-identifying data (and no oracle).
        assertThat(result.getResponse().getContentAsString()).isEmpty();
        User savedUser = userRepository.findByEmail(CANDIDATE_EMAIL).orElseThrow();
        assertThat(savedUser.getRoles()).contains(UserRole.CANDIDATE);
        assertThat(savedUser.isApproved()).isTrue();
        assertThat(savedUser.isEmailVerified()).isFalse();
        assertThat(savedUser.getResumeObjectKey())
                .startsWith("users/" + savedUser.getPublicId() + "/resume/")
                .endsWith(".pdf");
        // The CV was genuinely written to object storage, not just recorded on the user row.
        StatObjectResponse storedObject = minioClient.statObject(StatObjectArgs.builder()
                .bucket(storageProperties.getBucket())
                .object(savedUser.getResumeObjectKey())
                .build());
        assertThat(storedObject.size()).isEqualTo(PDF_BYTES.length);
        // Delivery is async and fires after the register transaction commits, so await it.
        verify(emailService, timeout(5_000)).sendEmailVerification(eq(CANDIDATE_EMAIL), any(), any());
    }

    @Test
    void registerWithAlreadyUsedEmailReturnsSameAcknowledgementWithoutCreatingADuplicate() throws Exception {
        registerCandidate();
        User original = userRepository.findByEmail(CANDIDATE_EMAIL).orElseThrow();

        // A second attempt on the taken email is indistinguishable from a fresh signup: 202 Accepted.
        registerCandidate();

        // No duplicate row, and the existing account is left untouched (same id).
        assertThat(userRepository.findByEmail(CANDIDATE_EMAIL).orElseThrow().getId()).isEqualTo(original.getId());
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void registerThenLoginIsBlockedUntilEmailIsVerified() throws Exception {
        registerCandidate();

        MvcResult blockedLogin = login(CANDIDATE_EMAIL, RAW_PASSWORD)
                .andExpect(status().isForbidden())
                .andReturn();
        assertThat(blockedLogin.getResponse().getContentAsString())
                .contains("\"errorCode\":\"EMAIL_NOT_VERIFIED\"");
        assertThat(setCookieHeaders(blockedLogin)).isEmpty();

        mockMvc.perform(withCsrf(post("/api/auth/verify-email"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + capturedVerificationToken() + "\"}"))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByEmail(CANDIDATE_EMAIL).orElseThrow().isEmailVerified()).isTrue();
        login(CANDIDATE_EMAIL, RAW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void verifyEmailTokenIsSingleUse() throws Exception {
        registerCandidate();
        String token = capturedVerificationToken();

        mockMvc.perform(withCsrf(post("/api/auth/verify-email"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + token + "\"}"))
                .andExpect(status().isNoContent());

        MvcResult reused = mockMvc.perform(withCsrf(post("/api/auth/verify-email"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + token + "\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(reused.getResponse().getContentAsString()).contains("\"errorCode\":\"INVALID_TOKEN\"");
    }

    @Test
    void verifyEmailWithUnknownTokenReturnsBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(post("/api/auth/verify-email"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"not-a-real-token\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"errorCode\":\"INVALID_TOKEN\"");
    }

    @Test
    void resendVerificationAlwaysReturnsNoContentAndOnlyEmailsUnverifiedAccounts() throws Exception {
        // Unknown email: 204, nothing sent — the endpoint must not leak which emails exist.
        mockMvc.perform(withCsrf(post("/api/auth/resend-verification"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"nobody@test.local\"}"))
                .andExpect(status().isNoContent());
        verify(emailService, org.mockito.Mockito.never()).sendEmailVerification(any(), any(), any());

        // Unverified account: 204 and a fresh link goes out.
        registerCandidate();
        mockMvc.perform(withCsrf(post("/api/auth/resend-verification"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + CANDIDATE_EMAIL + "\"}"))
                .andExpect(status().isNoContent());
        verify(emailService, timeout(5_000).times(2)).sendEmailVerification(eq(CANDIDATE_EMAIL), any(), any());

        // A resend invalidates earlier tokens: the newest link works, single-use as always.
        mockMvc.perform(withCsrf(post("/api/auth/verify-email"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + capturedVerificationToken() + "\"}"))
                .andExpect(status().isNoContent());
        assertThat(userRepository.findByEmail(CANDIDATE_EMAIL).orElseThrow().isEmailVerified()).isTrue();
    }

    private void registerCandidate() throws Exception {
        MockMultipartFile data = new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, """
                {
                  "firstName": "Test",
                  "lastName": "User",
                  "email": "%s",
                  "password": "%s",
                  "phoneNumber": "123456789"
                }
                """.formatted(CANDIDATE_EMAIL, RAW_PASSWORD).getBytes());
        MockMultipartFile resume = new MockMultipartFile("resume", "cv.pdf", "application/pdf", PDF_BYTES);

        mockMvc.perform(withCsrf(multipart("/api/auth/register").file(data).file(resume)))
                .andExpect(status().isNoContent());
    }

    /** Pulls the raw token out of the most recently emailed verification link. */
    private String capturedVerificationToken() {
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(5_000).atLeastOnce())
                .sendEmailVerification(eq(CANDIDATE_EMAIL), any(), linkCaptor.capture());
        String link = linkCaptor.getValue();
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    @Test
    void loginWithInvalidPasswordReturnsUnauthorizedWithoutCookies() throws Exception {
        createUser(true);

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
        createUser(true);
        Cookie accessCookie = loginAndGetCookie();

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
        createUser(true);
        Cookie accessCookie = loginAndGetCookie();

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
        createUser(true);
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
        Cookie rotatedRefreshCookie = cookie(refreshResult, "refresh_token");

        mockMvc.perform(withCsrf(post("/api/auth/refresh"))
                        .cookie(originalRefreshCookie))
                .andExpect(status().isUnauthorized());

        // Replaying a consumed token is treated as theft after rotation: the whole session
        // family is revoked, so even the legitimate successor token is dead.
        assertThat(refreshTokenRepository.findAll()).allSatisfy(token -> assertThat(token.isRevoked()).isTrue());
        mockMvc.perform(withCsrf(post("/api/auth/refresh"))
                        .cookie(rotatedRefreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithUnknownTokenDoesNotRevokeActiveSessions() throws Exception {
        createUser(true);
        MvcResult loginResult = login(CANDIDATE_EMAIL, RAW_PASSWORD)
                .andExpect(status().isOk())
                .andReturn();
        Cookie validRefreshCookie = cookie(loginResult, "refresh_token");

        // A token that never existed is not a reuse signal — just a plain rejection...
        mockMvc.perform(withCsrf(post("/api/auth/refresh"))
                        .cookie(new Cookie("refresh_token", "never-issued-token-value")))
                .andExpect(status().isUnauthorized());

        // ...so the real session must survive it.
        mockMvc.perform(withCsrf(post("/api/auth/refresh"))
                        .cookie(validRefreshCookie))
                .andExpect(status().isOk());
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
        createUser(true);
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

    private void createUser(boolean approved) {
        User user = User.builder()
                .email(AuthenticationControllerIntegrationTest.CANDIDATE_EMAIL)
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .firstName("Test")
                .lastName("User")
                .phoneNumber("123456789")
                .isApproved(approved)
                .emailVerified(true)
                .roles(Set.copyOf(List.of(UserRole.CANDIDATE)))
                .build();

        userRepository.save(user);
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

    private Cookie loginAndGetCookie() throws Exception {
        MvcResult loginResult = login(AuthenticationControllerIntegrationTest.CANDIDATE_EMAIL, AuthenticationControllerIntegrationTest.RAW_PASSWORD)
                .andExpect(status().isOk())
                .andReturn();

        return cookie(loginResult, "access_token");
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
