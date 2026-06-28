package com.krino.backend.controller;

import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.RemotePolicy;
import com.krino.backend.entity.enums.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JobControllerIntegrationTest extends AbstractControllerIntegrationTest
{
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
                                  "applicationDeadline": "2099-12-31T23:59:00Z",
                                  "salaryMin": 12000,
                                  "salaryMax": 18000,
                                  "salaryCurrency": "MAD",
                                  "salaryPeriod": "MONTHLY",
                                  "salaryVisible": true,
                                  "remotePolicy": "REMOTE",
                                  "experienceLevel": "MID_LEVEL",
                                  "openPositions": 2,
                                  "employmentType": "FULL_TIME",
                                  "contractType": "PERMANENT",
                                  "skills": [
                                    {
                                      "name": "Java",
                                      "importance": "REQUIRED"
                                    },
                                    {
                                      "name": "Spring Boot",
                                      "importance": "PREFERRED"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"title\":\"Backend Engineer\"");
        assertThat(result.getResponse().getContentAsString())
                .contains("\"salaryCurrency\":\"MAD\"")
                .contains("\"remotePolicy\":\"REMOTE\"")
                .contains("\"city\":null")
                .contains("\"status\":\"DRAFT\"")
                .contains("\"slug\":\"java\"")
                .contains("\"slug\":\"spring-boot\"")
                .contains("\"importance\":\"PREFERRED\"");
        assertThat(jobSkillRepository.count()).isEqualTo(2);
        assertThat(skillRepository.findBySlug("spring-boot")).isPresent();
        assertThat(jobRepository.findByTitle("Backend Engineer"))
                .singleElement()
                .satisfies(job -> {
                    assertThat(job.getCity()).isNull();
                    assertThat(job.getRemotePolicy().name()).isEqualTo("REMOTE");
                    assertThat(job.getOpenPositions()).isEqualTo(2);
                });
    }

    @Test
    void creatingJobWithDuplicateSkillsReturnsConflict() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        departmentRepository.save(department("Engineering"));
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(withCsrf(post("/api/jobs"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "Engineering",
                                  "title": "Backend Engineer",
                                  "applicationDeadline": "2099-12-31T23:59:00Z",
                                  "remotePolicy": "REMOTE",
                                  "employmentType": "FULL_TIME",
                                  "contractType": "PERMANENT",
                                  "skills": [
                                    {
                                      "name": "C#",
                                      "importance": "REQUIRED"
                                    },
                                    {
                                      "name": "C sharp",
                                      "importance": "PREFERRED"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isConflict());
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
                                  "applicationDeadline": "2099-12-31T23:59:00Z",
                                  "remotePolicy": "REMOTE",
                                  "employmentType": "FULL_TIME",
                                  "contractType": "PERMANENT"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingJobWithInvalidEmploymentTypeReturnsBadRequest() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        departmentRepository.save(department("Engineering"));
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(withCsrf(post("/api/jobs"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "Engineering",
                                  "title": "Backend Engineer",
                                  "applicationDeadline": "2099-12-31T23:59:00Z",
                                  "remotePolicy": "REMOTE",
                                  "employmentType": "NOT_A_TYPE",
                                  "contractType": "PERMANENT"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void puttingJobWithoutRequiredFieldsReturnsBadRequest() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Department department = departmentRepository.save(department("Engineering"));
        Job saved = job("Backend Engineer", department);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(withCsrf(put("/api/jobs/" + saved.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Build APIs"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchingJobWithBlankTitleReturnsBadRequest() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Department department = departmentRepository.save(department("Engineering"));
        Job saved = job("Backend Engineer", department);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(withCsrf(patch("/api/jobs/" + saved.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchingJobWithBlankEmploymentTypeReturnsBadRequest() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Department department = departmentRepository.save(department("Engineering"));
        Job saved = job("Backend Engineer", department);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(withCsrf(patch("/api/jobs/" + saved.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employmentType": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readingUnknownJobReturnsNotFound() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(get("/api/jobs/" + UUID.randomUUID())
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
                                  "applicationDeadline": "2099-12-31T23:59:00Z",
                                  "remotePolicy": "REMOTE",
                                  "employmentType": "FULL_TIME",
                                  "contractType": "PERMANENT"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void publishingDraftJobMakesItOpen() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Department department = departmentRepository.save(department("Engineering"));
        Job saved = job("Backend Engineer", department);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        MvcResult result = mockMvc.perform(withCsrf(post("/api/jobs/" + saved.getPublicId() + "/publish"))
                        .cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"status\":\"OPEN\"");
    }

    @Test
    void closingDraftJobMarksItFilled() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Department department = departmentRepository.save(department("Engineering"));
        Job saved = job("Backend Engineer", department);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        MvcResult result = mockMvc.perform(withCsrf(post("/api/jobs/" + saved.getPublicId() + "/close"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "FILLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"status\":\"FILLED\"");
    }

    @Test
    void pausingDraftJobReturnsConflict() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Department department = departmentRepository.save(department("Engineering"));
        Job saved = job("Backend Engineer", department);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        // Only an OPEN posting can be paused; pausing a DRAFT is an invalid transition.
        mockMvc.perform(withCsrf(post("/api/jobs/" + saved.getPublicId() + "/pause"))
                        .cookie(accessCookie))
                .andExpect(status().isConflict());
    }

    private Department department(String name)
    {
        Department department = new Department();
        department.setName(name);
        return department;
    }

    private Job job(String title, Department department)
    {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Job job = new Job("JOB-" + suffix, "job-" + suffix, department, title,
                EmploymentType.FULL_TIME, ContractType.PERMANENT, RemotePolicy.REMOTE);
        job.updateContent(title, "Build APIs");
        job.updateTimeline(Instant.parse("2099-12-31T23:59:00Z"), null);
        return jobRepository.save(job);
    }
}
