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
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
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

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void publicListingIsServedFromCacheAndEvictedOnMutation() throws Exception {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Job job = openJob();
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        listPublicJobs().andExpect(jsonPath("$.content[0].title").value("Backend Engineer"));

        // Renaming through the repository skips the service, so nothing evicts the listing.
        // The stale title coming back proves the response was deserialized out of Redis.
        job.updateContent("Renamed Behind The Cache", job.getDescription());
        jobRepository.save(job);
        listPublicJobs().andExpect(jsonPath("$.content[0].title").value("Backend Engineer"));

        mockMvc.perform(withCsrf(patch("/api/jobs/" + job.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Platform Engineer"
                                }
                                """))
                .andExpect(status().isOk());

        // Going through the service evicts, so the next read sees the new title immediately.
        listPublicJobs().andExpect(jsonPath("$.content[0].title").value("Platform Engineer"));
    }

    @Test
    void onlyTheFirstPageOfThePublicListingIsCached() throws Exception {
        openJob();

        // The listing is anonymous, so an unauthenticated caller controls the page number.
        // Caching every page would let one client mint unbounded Redis keys by walking it.
        mockMvc.perform(get("/api/public/jobs").param("page", "3")).andExpect(status().isOk());
        assertThat(cachedListingKeys()).isEmpty();

        listPublicJobs().andExpect(status().isOk());
        assertThat(cachedListingKeys()).hasSize(1);
    }

    @Test
    void staffListingIsNotCached() throws Exception {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        openJob();

        mockMvc.perform(get("/api/jobs").cookie(loginAndGetAccessCookie(ADMIN_EMAIL)))
                .andExpect(status().isOk());

        // Staff results include drafts; caching them is one lookup mistake away from serving
        // a draft to a candidate, so this listing deliberately has no cache entry at all.
        assertThat(cachedListingKeys()).isEmpty();
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

    private Set<byte[]> cachedListingKeys() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            return connection.keyCommands()
                    .keys(("krino::" + JOB_LISTINGS_CACHE + "*").getBytes(StandardCharsets.UTF_8));
        }
    }

    private org.springframework.test.web.servlet.ResultActions listPublicJobs() throws Exception {
        return mockMvc.perform(get("/api/public/jobs")).andExpect(status().isOk());
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

    private Job openJob() {
        Job job = job();
        job.publish(Instant.now());
        return jobRepository.save(job);
    }
}
