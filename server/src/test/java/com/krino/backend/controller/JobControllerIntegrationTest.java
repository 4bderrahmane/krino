package com.krino.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.repository.DepartmentRepository;
import com.krino.backend.repository.JobRepository;
import com.krino.backend.repository.RefreshTokenRepository;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class JobControllerIntegrationTest
{
    private static final String RAW_PASSWORD = "Password123!";
    private static final String ADMIN_EMAIL = "admin@test.local";
    private static final String CANDIDATE_EMAIL = "candidate@test.local";

    private final JobRepository jobRepository;
    private final DepartmentRepository departmentRepository;
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
        jobRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminCreatesJobReturnsCreatedAndPersists() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        departmentRepository.save(department("Engineering"));
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        MvcResult result = mockMvc.perform(withCsrf(post("/api/jobs"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "Engineering",
                                  "title": "Backend Engineer",
                                  "description": "Build APIs",
                                  "applyingDeadline": "2099-12-31",
                                  "employmentType": "FULL_TIME",
                                  "contractType": "PERMANENT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"title\":\"Backend Engineer\"");
        assertThat(jobRepository.findByTitle("Backend Engineer")).hasSize(1);
    }

    @Test
    void creatingJobForUnknownDepartmentReturnsNotFound() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(withCsrf(post("/api/jobs"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "Ghost",
                                  "title": "Backend Engineer",
                                  "applyingDeadline": "2099-12-31",
                                  "employmentType": "FULL_TIME",
                                  "contractType": "PERMANENT"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void readingUnknownJobReturnsNotFound() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(get("/api/jobs/" + java.util.UUID.randomUUID())
                        .cookie(accessCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestWithoutAuthenticationReturnsUnauthorized() throws Exception
    {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void candidateCannotCreateJob() throws Exception
    {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        departmentRepository.save(department("Engineering"));
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(post("/api/jobs"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "Engineering",
                                  "title": "Backend Engineer",
                                  "applyingDeadline": "2099-12-31",
                                  "employmentType": "FULL_TIME",
                                  "contractType": "PERMANENT"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private Department department(String name)
    {
        Department department = new Department();
        department.setName(name);
        return department;
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
