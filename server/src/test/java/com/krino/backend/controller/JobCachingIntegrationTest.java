package com.krino.backend.controller;

import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.RemotePolicy;
import com.krino.backend.entity.enums.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static com.krino.backend.configuration.CachingConfiguration.JOBS_CACHE;
import static com.krino.backend.configuration.CachingConfiguration.JOB_LISTINGS_CACHE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JobCachingIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void listingIsCachedAndEvictedOnMutation() throws Exception {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Job job = job();
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        // First read populates the cache; second read is served from Redis and must
        // deserialize back to the same payload.
        listJobs(accessCookie).andExpect(jsonPath("$.content[0].title").value("Backend Engineer"));
        assertThat(Objects.requireNonNull(cacheManager.getCache(JOB_LISTINGS_CACHE)).get("all")).isNotNull();
        listJobs(accessCookie).andExpect(jsonPath("$.content[0].title").value("Backend Engineer"));

        mockMvc.perform(withCsrf(patch("/api/jobs/" + job.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Platform Engineer"
                                }
                                """))
                .andExpect(status().isOk());

        // The mutation evicted the listing, so the next read sees the new title immediately.
        assertThat(Objects.requireNonNull(cacheManager.getCache(JOB_LISTINGS_CACHE)).get("all")).isNull();
        listJobs(accessCookie).andExpect(jsonPath("$.content[0].title").value("Platform Engineer"));
    }

    @Test
    void jobDetailIsCachedByPublicIdAndEvictedOnMutation() throws Exception {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Job job = job();
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        getJob(accessCookie, job.getPublicId()).andExpect(jsonPath("$.title").value("Backend Engineer"));
        assertThat(cacheManager.getCache(JOBS_CACHE).get(job.getPublicId())).isNotNull();
        getJob(accessCookie, job.getPublicId()).andExpect(jsonPath("$.title").value("Backend Engineer"));

        mockMvc.perform(withCsrf(patch("/api/jobs/" + job.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Platform Engineer"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(cacheManager.getCache(JOBS_CACHE).get(job.getPublicId())).isNull();
        getJob(accessCookie, job.getPublicId()).andExpect(jsonPath("$.title").value("Platform Engineer"));
    }

    private org.springframework.test.web.servlet.ResultActions listJobs(Cookie accessCookie) throws Exception {
        return mockMvc.perform(get("/api/jobs").cookie(accessCookie))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions getJob(Cookie accessCookie, UUID publicId) throws Exception {
        return mockMvc.perform(get("/api/jobs/" + publicId).cookie(accessCookie))
                .andExpect(status().isOk());
    }

    private Job job() {
        Department department = new Department();
        department.setName("Engineering");
        department = departmentRepository.save(department);

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Job job = new Job("JOB-" + suffix, "job-" + suffix, department, "Backend Engineer",
                EmploymentType.FULL_TIME, ContractType.PERMANENT, RemotePolicy.REMOTE);
        job.updateContent("Backend Engineer", "Build APIs");
        job.updateTimeline(Instant.parse("2099-12-31T23:59:00Z"), null);
        return jobRepository.save(job);
    }
}
