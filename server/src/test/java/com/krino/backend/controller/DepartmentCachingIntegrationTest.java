package com.krino.backend.controller;

import com.krino.backend.entity.Department;
import com.krino.backend.entity.enums.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DepartmentCachingIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void listingIsServedFromCacheAndSurvivesTheRoundTrip() throws Exception {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Department department = department("Engineering");
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        listDepartments(accessCookie).andExpect(jsonPath("$[0].name").value("Engineering"));

        // Renaming through the repository skips the service, so nothing evicts the listing.
        // The next read can therefore only answer "Engineering" by deserializing what Redis
        // holds, which is what proves the cached payload survived the round trip.
        department.setName("Platform");
        departmentRepository.save(department);

        listDepartments(accessCookie).andExpect(jsonPath("$[0].name").value("Engineering"));
    }

    @Test
    void listingIsEvictedOnMutation() throws Exception {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Department department = department("Engineering");
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        listDepartments(accessCookie).andExpect(jsonPath("$[0].name").value("Engineering"));

        mockMvc.perform(withCsrf(patch("/api/departments/" + department.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Platform"
                                }
                                """))
                .andExpect(status().isOk());

        listDepartments(accessCookie).andExpect(jsonPath("$[0].name").value("Platform"));
    }

    private org.springframework.test.web.servlet.ResultActions listDepartments(Cookie accessCookie) throws Exception {
        return mockMvc.perform(get("/api/departments").cookie(accessCookie))
                .andExpect(status().isOk());
    }

    private Department department(String name) {
        Department department = new Department();
        department.setName(name);
        return departmentRepository.save(department);
    }
}
