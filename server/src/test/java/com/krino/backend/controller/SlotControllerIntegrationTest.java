package com.krino.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.repository.RefreshTokenRepository;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class SlotControllerIntegrationTest
{
    private static final String RAW_PASSWORD = "Password123!";
    private static final String ADMIN_EMAIL = "admin@test.local";
    private static final String INTERVIEWER_EMAIL = "interviewer@test.local";
    private static final String CANDIDATE_EMAIL = "candidate@test.local";

    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final WebApplicationContext webApplicationContext;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        refreshTokenRepository.deleteAll();
        slotRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminCreatesSlotForInterviewerReturnsCreatedAndPersists() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(withCsrf(post("/api/slots"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "interviewerId": "%s",
                                  "interviewDate": "2099-01-01",
                                  "startTime": "09:00:00",
                                  "endTime": "09:45:00"
                                }
                                """.formatted(interviewer.getPublicId())))
                .andExpect(status().isCreated());

        assertThat(slotRepository.count()).isEqualTo(1);
    }

    @Test
    void adminReadsSlotByPublicIdIncludingInterviewer() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        Slot slot = slotRepository.save(slotFor(interviewer));

        MvcResult result = mockMvc.perform(get("/api/slots/" + slot.getPublicId())
                        .cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"id\":\"" + slot.getPublicId() + "\"",
                "\"durationInMinutes\":45",
                "\"email\":\"" + INTERVIEWER_EMAIL + "\""
        );
    }

    @Test
    void readingUnknownSlotReturnsNotFound() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(get("/api/slots/" + UUID.randomUUID())
                        .cookie(accessCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminDeletesSlotReturnsNoContent() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        Slot slot = slotRepository.save(slotFor(interviewer));

        mockMvc.perform(withCsrf(delete("/api/slots/" + slot.getPublicId()))
                        .cookie(accessCookie))
                .andExpect(status().isNoContent());

        assertThat(slotRepository.count()).isZero();
    }

    @Test
    void requestWithoutAuthenticationReturnsUnauthorized() throws Exception
    {
        mockMvc.perform(get("/api/slots"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void candidateCannotCreateSlot() throws Exception
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(post("/api/slots"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "interviewerId": "%s",
                                  "interviewDate": "2099-01-01",
                                  "startTime": "09:00:00",
                                  "endTime": "09:45:00"
                                }
                                """.formatted(candidate.getPublicId())))
                .andExpect(status().isForbidden());
    }

    private Slot slotFor(User interviewer)
    {
        Slot slot = new Slot();
        slot.setInterviewer(interviewer);
        slot.setInterviewDate(LocalDate.of(2099, 1, 1));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(9, 45));
        return slot;
    }

    private User createUser(String email, boolean approved, UserRole... roles)
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

    private Cookie loginAndGetAccessCookie(String email) throws Exception
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

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception
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
