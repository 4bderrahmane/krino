package com.krino.backend.controller;

import com.krino.backend.entity.Department;
import com.krino.backend.entity.enums.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DepartmentControllerIntegrationTest extends AbstractControllerIntegrationTest
{
    @Test
    void adminCreatesDepartmentReturnsCreatedAndPersists() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        MvcResult result = mockMvc.perform(withCsrf(post("/api/departments"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Engineering",
                                  "description": "Builds the product"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"name\":\"Engineering\"");
        assertThat(departmentRepository.findByName("Engineering")).isPresent();
    }

    @Test
    void adminReadsDepartmentByPublicId() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        Department saved = departmentRepository.save(department("Finance", "Money matters"));

        MvcResult result = mockMvc.perform(get("/api/departments/" + saved.getPublicId())
                        .cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"id\":\"" + saved.getPublicId() + "\"",
                "\"name\":\"Finance\""
        );
    }

    @Test
    void creatingDuplicateDepartmentNameReturnsConflict() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        departmentRepository.save(department("Engineering", "Existing"));

        mockMvc.perform(withCsrf(post("/api/departments"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Engineering",
                                  "description": "Duplicate"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void adminDeletesDepartmentReturnsNoContent() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        Department saved = departmentRepository.save(department("Marketing", "Promotes"));

        mockMvc.perform(withCsrf(delete("/api/departments/" + saved.getPublicId()))
                        .cookie(accessCookie))
                .andExpect(status().isNoContent());

        assertThat(departmentRepository.findByName("Marketing")).isEmpty();
    }

    @Test
    void readingUnknownDepartmentReturnsNotFound() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(get("/api/departments/" + UUID.randomUUID())
                        .cookie(accessCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestWithoutAuthenticationReturnsUnauthorized() throws Exception
    {
        mockMvc.perform(get("/api/departments/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void candidateCannotCreateDepartment() throws Exception
    {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(post("/api/departments"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Engineering",
                                  "description": "Builds the product"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private Department department(String name, String description)
    {
        Department department = new Department();
        department.setName(name);
        department.setDescription(description);
        return department;
    }
}
