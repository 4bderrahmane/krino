package com.krino.backend.controller;

import com.krino.backend.entity.Department;
import com.krino.backend.entity.enums.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
