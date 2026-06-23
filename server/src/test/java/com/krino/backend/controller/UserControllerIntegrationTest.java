package com.krino.backend.controller;

import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends AbstractControllerIntegrationTest
{
    @Test
    void adminListsAllUsers() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        MvcResult result = mockMvc.perform(get("/api/users").cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"email\":\"" + ADMIN_EMAIL + "\"",
                "\"email\":\"" + CANDIDATE_EMAIL + "\""
        );
    }

    @Test
    void adminReadsUserByPublicId() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        MvcResult result = mockMvc.perform(get("/api/users/" + candidate.getPublicId()).cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"email\":\"" + CANDIDATE_EMAIL + "\"");
    }

    @Test
    void candidateReadsOwnProfileByPublicId() throws Exception
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(get("/api/users/" + candidate.getPublicId()).cookie(accessCookie))
                .andExpect(status().isOk());
    }

    @Test
    void candidateCannotReadAnotherUsersProfile() throws Exception
    {
        User admin = createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(get("/api/users/" + admin.getPublicId()).cookie(accessCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminApprovesPendingUser() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User pending = createUser(CANDIDATE_EMAIL, false, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(withCsrf(patch("/api/users/" + pending.getPublicId() + "/approval")).cookie(accessCookie))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByEmail(CANDIDATE_EMAIL)).get()
                .extracting(User::isApproved).isEqualTo(true);
    }

    @Test
    void adminListsNonApprovedUsers() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        createUser(CANDIDATE_EMAIL, false, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        MvcResult result = mockMvc.perform(get("/api/users/non-approved").cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"email\":\"" + CANDIDATE_EMAIL + "\"");
    }

    @Test
    void candidateUpdatesOwnAccountPartially() throws Exception
    {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(patch("/api/users/me"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Updated"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(userRepository.findByEmail(CANDIDATE_EMAIL)).get()
                .extracting(User::getFirstName).isEqualTo("Updated");
    }

    @Test
    void candidateDeletesOwnAccount() throws Exception
    {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(delete("/api/users/me")).cookie(accessCookie))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByEmail(CANDIDATE_EMAIL)).isEmpty();
    }
}
