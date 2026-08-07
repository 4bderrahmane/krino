package com.krino.backend.controller;

import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.JobStatus;
import com.krino.backend.entity.enums.RemotePolicy;
import com.krino.backend.entity.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The public department list is the filter beside the public job list, so it has to agree with
 * it: a department is listed only while it has a published opening.
 */
class DepartmentVisibilityIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void anonymousVisitorSeesOnlyDepartmentsWithAnOpening() throws Exception {
        Department hiring = department("Engineering");
        department("Skunkworks");
        openJob(hiring, "Backend Engineer");

        mockMvc.perform(get("/api/public/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Engineering"));
    }

    @Test
    void aDepartmentWithOnlyDraftsStaysHidden() throws Exception {
        Department secretive = department("Skunkworks");
        draftJob(secretive, "Unannounced Role");

        // Listing it would re-leak exactly what hiding the draft was meant to conceal.
        mockMvc.perform(get("/api/public/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aDepartmentDropsOutOfTheListWhenItsLastOpeningCloses() throws Exception {
        Department hiring = department("Engineering");
        Job job = openJob(hiring, "Backend Engineer");

        mockMvc.perform(get("/api/public/departments"))
                .andExpect(jsonPath("$.length()").value(1));

        job.close(JobStatus.FILLED, Instant.now());
        jobRepository.save(job);

        // This list is uncached precisely so a job lifecycle change shows up here at once,
        // without every mutation in JobService owing it an eviction.
        mockMvc.perform(get("/api/public/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void departmentWithSeveralOpeningsIsListedOnce() throws Exception {
        Department hiring = department("Engineering");
        openJob(hiring, "Backend Engineer");
        openJob(hiring, "Frontend Engineer");

        mockMvc.perform(get("/api/public/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void internalDirectoryIsStaffOnly() throws Exception {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        department("Skunkworks");

        mockMvc.perform(get("/api/departments").cookie(loginAndGetAccessCookie(ADMIN_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Skunkworks"));

        mockMvc.perform(get("/api/departments").cookie(loginAndGetAccessCookie(CANDIDATE_EMAIL)))
                .andExpect(status().isForbidden());
    }

    /**
     * Neither list is paged, so neither takes a client-supplied sort: the order is fixed by name
     * in the query. A stray {@code ?sort=} or {@code ?page=} is just an unknown parameter now,
     * and must not change the answer.
     */
    @Test
    void bothListsAreWholeArraysOrderedByName() throws Exception {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Department platform = department("Platform");
        Department engineering = department("Engineering");
        openJob(platform, "SRE");
        openJob(engineering, "Backend Engineer");

        mockMvc.perform(get("/api/public/departments").param("sort", "description").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Engineering"))
                .andExpect(jsonPath("$[1].name").value("Platform"));

        mockMvc.perform(get("/api/departments").cookie(loginAndGetAccessCookie(ADMIN_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Engineering"))
                .andExpect(jsonPath("$[1].name").value("Platform"));
    }

    private Department department(String name) {
        Department department = new Department();
        department.setName(name);
        return departmentRepository.save(department);
    }

    private Job draftJob(Department department, String title) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Job job = new Job("JOB-" + suffix, "job-" + suffix, department, title,
                EmploymentType.FULL_TIME, ContractType.PERMANENT, RemotePolicy.REMOTE);
        job.updateContent(title, "Build APIs");
        job.updateTimeline(Instant.parse("2099-12-31T23:59:00Z"), null);
        return jobRepository.save(job);
    }

    private Job openJob(Department department, String title) {
        Job job = draftJob(department, title);
        job.publish(Instant.now());
        return jobRepository.save(job);
    }
}
