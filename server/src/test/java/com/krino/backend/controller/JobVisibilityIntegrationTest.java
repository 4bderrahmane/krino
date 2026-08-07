package com.krino.backend.controller;

import com.krino.backend.entity.Application;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.JobStatus;
import com.krino.backend.entity.enums.RemotePolicy;
import com.krino.backend.entity.enums.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The catalogue has two audiences. {@code /api/public/jobs} is the anonymous storefront and
 * shows published postings only; {@code /api/jobs} is the internal view and shows drafts, so it
 * is staff-only. These tests pin the boundary between them.
 */
class JobVisibilityIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void anonymousVisitorSeesPublishedPostingsOnly() throws Exception {
        openJob("Backend Engineer");
        draftJob("Secret Skunkworks Role");

        mockMvc.perform(get("/api/public/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Backend Engineer"));
    }

    @Test
    void anonymousVisitorCanReadAPublishedPosting() throws Exception {
        Job job = openJob("Backend Engineer");

        mockMvc.perform(get("/api/public/jobs/" + job.getPublicId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Backend Engineer"));
    }

    @Test
    void anonymousVisitorGetsNotFoundForADraft() throws Exception {
        Job draft = draftJob("Secret Skunkworks Role");

        // 404 rather than 403: a 403 would confirm the draft exists.
        mockMvc.perform(get("/api/public/jobs/" + draft.getPublicId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void staleSessionCookieDoesNotBlockPublicBrowsing() throws Exception {
        openJob("Backend Engineer");

        // Browsers keep sending an expired access_token long after the session lapses. The
        // public catalogue needs no session, so a rejected token must not turn into a 401 here.
        mockMvc.perform(get("/api/public/jobs").cookie(new Cookie("access_token", "expired.rubbish.token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void staffCatalogueShowsDraftsAndCandidatesCannotReachIt() throws Exception {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        draftJob("Secret Skunkworks Role");

        mockMvc.perform(get("/api/jobs").cookie(loginAndGetAccessCookie(ADMIN_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Secret Skunkworks Role"));

        mockMvc.perform(get("/api/jobs").cookie(loginAndGetAccessCookie(CANDIDATE_EMAIL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void candidateCannotReadADraftByPublicId() throws Exception {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Job draft = draftJob("Secret Skunkworks Role");

        mockMvc.perform(get("/api/jobs/" + draft.getPublicId())
                        .cookie(loginAndGetAccessCookie(CANDIDATE_EMAIL)))
                .andExpect(status().isNotFound());
    }

    @Test
    void candidateKeepsAccessToAPostingTheyAppliedToAfterItCloses() throws Exception {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Job job = openJob("Backend Engineer");

        Application application = new Application();
        application.setJob(job);
        application.setCandidate(candidate);
        applicationRepository.save(application);

        job.close(JobStatus.FILLED, Instant.now());
        jobRepository.save(job);

        // Otherwise the candidate's own application list would start pointing at 404s.
        mockMvc.perform(get("/api/jobs/" + job.getPublicId())
                        .cookie(loginAndGetAccessCookie(CANDIDATE_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FILLED"));

        // The storefront still hides it: it is no longer a published opening.
        mockMvc.perform(get("/api/public/jobs/" + job.getPublicId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listingPaginatesInTheDatabase() throws Exception {
        openJob("Backend Engineer");
        openJob("Frontend Engineer");
        openJob("Platform Engineer");

        mockMvc.perform(get("/api/public/jobs").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        mockMvc.perform(get("/api/public/jobs").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void oversizedPageRequestIsCapped() throws Exception {
        openJob("Backend Engineer");

        mockMvc.perform(get("/api/public/jobs").param("size", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(100));
    }

    @Test
    void sortingByANonWhitelistedColumnIsRejected() throws Exception {
        openJob("Backend Engineer");

        mockMvc.perform(get("/api/public/jobs").param("sort", "salaryMax"))
                .andExpect(status().isBadRequest());
    }

    private Job draftJob(String title) {
        Department department = departmentRepository.findByName("Engineering")
                .orElseGet(() -> {
                    Department created = new Department();
                    created.setName("Engineering");
                    return departmentRepository.save(created);
                });

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Job job = new Job("JOB-" + suffix, "job-" + suffix, department, title,
                EmploymentType.FULL_TIME, ContractType.PERMANENT, RemotePolicy.REMOTE);
        job.updateContent(title, "Build APIs");
        job.updateTimeline(Instant.parse("2099-12-31T23:59:00Z"), null);
        return jobRepository.save(job);
    }

    private Job openJob(String title) {
        Job job = draftJob(title);
        job.publish(Instant.now());
        return jobRepository.save(job);
    }
}
