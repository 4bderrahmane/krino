package com.krino.backend.controller;

import com.krino.backend.entity.Application;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.support.TestJobs;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static java.nio.charset.StandardCharsets.UTF_8;

class ApplicationControllerIntegrationTest extends AbstractControllerIntegrationTest
{
    private static final String OTHER_CANDIDATE_EMAIL = "other-candidate@test.local";

    @Test
    void candidateAppliesToOpenJobReturnsCreatedAndPersists() throws Exception
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Job job = openJob();
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(post("/api/applications"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobPublicId": "%s"
                                }
                                """.formatted(job.getPublicId())))
                .andExpect(status().isCreated());

        assertThat(applicationRepository.findByJobAndCandidate(job, candidate)).isPresent();
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
                  "jobPublicId": "%s"
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
                                  "jobPublicId": "%s"
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
    void candidateCanFindOnlyTheirOwnApplicationForAJob() throws Exception
    {
        User owner = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        createUser(OTHER_CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Job job = openJob();
        Application application = applicationRepository.save(applicationFor(job, owner));
        String path = "/api/applications/me/by-job/" + job.getPublicId();

        Cookie ownerCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);
        mockMvc.perform(get(path).cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(application.getPublicId().toString()))
                .andExpect(jsonPath("$.resume").isEmpty());

        Cookie otherCookie = loginAndGetAccessCookie(OTHER_CANDIDATE_EMAIL);
        mockMvc.perform(get(path).cookie(otherCookie))
                .andExpect(status().isNotFound());
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

    @Test
    void hrDecisionIsReturnedAndPersisted() throws Exception
    {
        createUser("hr@test.local", true, UserRole.HR_MANAGER);
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Application application = applicationRepository.save(applicationFor(openJob(), candidate));
        Cookie hrCookie = loginAndGetAccessCookie("hr@test.local");

        mockMvc.perform(withCsrf(patch("/api/applications/" + application.getPublicId()))
                        .cookie(hrCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/applications/" + application.getPublicId()).cookie(hrCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void candidateCannotChangeDecisionOrMutateAnotherCandidatesApplication() throws Exception
    {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        User owner = createUser(OTHER_CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Application application = applicationRepository.save(applicationFor(openJob(), owner));
        Cookie candidateCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(patch("/api/applications/" + application.getPublicId()))
                        .cookie(candidateCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(withCsrf(delete("/api/applications/" + application.getPublicId()))
                        .cookie(candidateCookie))
                .andExpect(status().isForbidden());
        mockMvc.perform(withCsrf(multipart(HttpMethod.PUT, "/api/applications/" + application.getPublicId() + "/resume"))
                        .file(pdf("other.pdf"))
                        .cookie(candidateCookie))
                .andExpect(status().isForbidden());

        Cookie ownerCookie = loginAndGetAccessCookie(OTHER_CANDIDATE_EMAIL);
        mockMvc.perform(withCsrf(patch("/api/applications/" + application.getPublicId()))
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void ownerReplacesResumeThenDeletesApplication() throws Exception
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Application application = applicationRepository.save(applicationFor(openJob(), candidate));
        Cookie cookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);
        String path = "/api/applications/" + application.getPublicId();

        mockMvc.perform(withCsrf(multipart(HttpMethod.PUT, path + "/resume")).file(pdf("first.pdf")).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resume.originalFilename").value("first.pdf"));
        mockMvc.perform(withCsrf(multipart(HttpMethod.PUT, path + "/resume")).file(pdf("replacement.pdf")).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resume.originalFilename").value("replacement.pdf"));
        mockMvc.perform(get(path).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resume.originalFilename").value("replacement.pdf"));
        mockMvc.perform(withCsrf(delete(path)).cookie(cookie))
                .andExpect(status().isNoContent());
        assertThat(applicationRepository.findByPublicId(application.getPublicId())).isEmpty();
    }

    @Test
    void schedulingOptionsSearchAndPageBeyondFirstHundred() throws Exception
    {
        createUser("hr@test.local", true, UserRole.HR_MANAGER);
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Department department = departmentRepository.save(department());
        Application last = null;
        for (int i = 0; i < 105; i++) {
            Job job = jobRepository.save(TestJobs.open(department, i == 104 ? "Needle Engineer" : "Engineer " + i));
            last = applicationRepository.save(applicationFor(job, candidate));
        }
        Cookie hr = loginAndGetAccessCookie("hr@test.local");

        mockMvc.perform(get("/api/applications/scheduling-options?page=5&size=20").cookie(hr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(105))
                .andExpect(jsonPath("$.content.length()").value(5));
        mockMvc.perform(get("/api/applications/scheduling-options?search=needle&page=0&size=20").cookie(hr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(last.getPublicId().toString()));

        Cookie candidateCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);
        mockMvc.perform(get("/api/applications/scheduling-options").cookie(candidateCookie))
                .andExpect(status().isForbidden());
    }

    private MockMultipartFile pdf(String filename)
    {
        return new MockMultipartFile("resume", filename, "application/pdf", "%PDF-1.4\ncontent".getBytes(UTF_8));
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
