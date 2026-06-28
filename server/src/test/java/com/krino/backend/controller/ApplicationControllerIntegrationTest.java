package com.krino.backend.controller;

import com.krino.backend.entity.Application;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.support.TestJobs;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationControllerIntegrationTest extends AbstractControllerIntegrationTest
{
    private static final String OTHER_CANDIDATE_EMAIL = "other-candidate@test.local";

    @Test
    void candidateAppliesToOpenJobReturnsCreatedAndPersists() throws Exception
    {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Job job = openJob();
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(post("/api/applications"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": "%s"
                                }
                                """.formatted(job.getPublicId())))
                .andExpect(status().isCreated());

        assertThat(applicationRepository.count()).isEqualTo(1);
    }

    @Test
    void applyingTwiceToSameJobReturnsConflict() throws Exception
    {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Job job = openJob();
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        String body = """
                {
                  "jobId": "%s"
                }
                """.formatted(job.getPublicId());

        mockMvc.perform(withCsrf(post("/api/applications"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(withCsrf(post("/api/applications"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void applyingToUnknownJobReturnsNotFound() throws Exception
    {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(post("/api/applications"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": "%s"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void candidateReadsApplicationIncludingJobAndCandidate() throws Exception
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Job job = openJob();
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);
        Application application = applicationRepository.save(applicationFor(job, candidate));

        MvcResult result = mockMvc.perform(get("/api/applications/" + application.getPublicId())
                        .cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"id\":\"" + application.getPublicId() + "\"",
                "\"jobId\":\"" + job.getPublicId() + "\"",
                "\"email\":\"" + CANDIDATE_EMAIL + "\""
        );
    }

    @Test
    void candidateCannotReadAnotherCandidatesApplication() throws Exception
    {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        User otherCandidate = createUser(OTHER_CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Job job = openJob();
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);
        Application application = applicationRepository.save(applicationFor(job, otherCandidate));

        mockMvc.perform(get("/api/applications/" + application.getPublicId())
                        .cookie(accessCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void readingUnknownApplicationReturnsNotFound() throws Exception
    {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(get("/api/applications/" + UUID.randomUUID())
                        .cookie(accessCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestWithoutAuthenticationReturnsUnauthorized() throws Exception
    {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized());
    }

    private Job openJob()
    {
        Department department = departmentRepository.save(department());
        return jobRepository.save(TestJobs.open(department, "Backend Engineer"));
    }

    private Department department()
    {
        Department department = new Department();
        department.setName("Engineering");
        return department;
    }

    private Application applicationFor(Job job, User candidate)
    {
        Application application = new Application();
        application.setJob(job);
        application.setCandidate(candidate);
        return application;
    }
}
