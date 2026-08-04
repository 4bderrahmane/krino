package com.krino.backend.controller;

import com.krino.backend.entity.EmailVerificationToken;
import com.krino.backend.entity.PasswordResetToken;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends AbstractControllerIntegrationTest
{
    private static final String OTHER_CANDIDATE_EMAIL = "other-candidate@test.local";

    /** The token tables CHECK that the hash is exactly 32 bytes. */
    private static byte[] tokenHashOf(byte fill)
    {
        byte[] hash = new byte[32];
        Arrays.fill(hash, fill);
        return hash;
    }

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
    void adminSortsUsersByWhitelistedProperty() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        createUser(CANDIDATE_EMAIL, false, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        // isApproved is a boolean column on the whitelist: proves the allowed name
        // actually resolves against the persistence metamodel, not just past the guard.
        mockMvc.perform(get("/api/users").param("sort", "isApproved,desc").cookie(accessCookie))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsSortByPropertyOutsideTheWhitelist() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        // password is a real column but not sortable: the whitelist must reject it with a
        // 400 before it ever reaches the query.
        mockMvc.perform(get("/api/users").param("sort", "password,asc").cookie(accessCookie))
                .andExpect(status().isBadRequest());
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

        mockMvc.perform(withCsrf(patch("/api/users/" + pending.getPublicId() + "/approval"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approved": true
                                }
                                """))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByEmail(CANDIDATE_EMAIL)).get()
                .extracting(User::isApproved).isEqualTo(true);
    }

    @Test
    void adminRevokesApproval() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User approved = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(withCsrf(patch("/api/users/" + approved.getPublicId() + "/approval"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approved": false
                                }
                                """))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByEmail(CANDIDATE_EMAIL)).get()
                .extracting(User::isApproved).isEqualTo(false);
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
    void changingEmailResetsVerificationAndBlocksNextLogin() throws Exception
    {
        String newEmail = "new-address@test.local";
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(patch("/api/users/me"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s"
                                }
                                """.formatted(newEmail)))
                .andExpect(status().isOk());

        // The new address is unproven until its emailed link is used.
        User updated = userRepository.findByEmail(newEmail).orElseThrow();
        assertThat(updated.isEmailVerified()).isFalse();
        assertThat(emailVerificationTokenRepository.count()).isEqualTo(1L);

        // The session issued before the change is grandfathered...
        mockMvc.perform(get("/api/users/me").cookie(accessCookie))
                .andExpect(status().isOk());

        // ...but a fresh password login is refused until the new address is verified.
        mockMvc.perform(withCsrf(post("/api/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(newEmail, RAW_PASSWORD)))
                .andExpect(status().isForbidden());
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

    @Test
    void candidateCanPatchOwnUserByPublicId() throws Exception
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(patch("/api/users/" + candidate.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Updated"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(userRepository.findByPublicId(candidate.getPublicId()).orElseThrow().getFirstName())
                .isEqualTo("Updated");
    }

    @Test
    void candidateCannotPatchAnotherUserByPublicId() throws Exception
    {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        User otherCandidate = createUser(OTHER_CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(patch("/api/users/" + otherCandidate.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Stolen"
                                }
                                """))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findByPublicId(otherCandidate.getPublicId()).orElseThrow().getFirstName())
                .isEqualTo("Test");
    }

    @Test
    void interviewerCannotReadArbitraryUserByPublicId() throws Exception
    {
        createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(INTERVIEWER_EMAIL);

        mockMvc.perform(get("/api/users/" + candidate.getPublicId())
                        .cookie(accessCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void userWithLongEmailCanPerformAuditedWrites() throws Exception
    {
        String longEmail = "a".repeat(64) + "@really-quite-long-domain-name.example.local";
        assertThat(longEmail.length()).isGreaterThan(100);

        User candidate = createUser(longEmail, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(longEmail);

        mockMvc.perform(withCsrf(patch("/api/users/" + candidate.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Updated"
                                }
                                """))
                .andExpect(status().isOk());

        User reloaded = userRepository.findByPublicId(candidate.getPublicId()).orElseThrow();
        assertThat(reloaded.getFirstName()).isEqualTo("Updated");
        assertThat(reloaded.getLastModifiedBy()).isEqualTo(longEmail);
    }

    @Test
    void candidateWithVerificationAndResetTokensCanStillBeDeleted() throws Exception
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        // Consuming a token only flips `used`, so the row outlives it until the nightly purge.
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .tokenHash(tokenHashOf((byte) 1))
                .user(candidate)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(true)
                .build());
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .tokenHash(tokenHashOf((byte) 2))
                .user(candidate)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(true)
                .build());
        assertThat(emailVerificationTokenRepository.count()).isEqualTo(1);
        assertThat(passwordResetTokenRepository.count()).isEqualTo(1);

        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(delete("/api/users/me")).cookie(accessCookie))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByEmail(CANDIDATE_EMAIL)).isEmpty();
        assertThat(emailVerificationTokenRepository.count()).isZero();
        assertThat(passwordResetTokenRepository.count()).isZero();
    }

    @Test
    void changingPasswordRevokesEveryRefreshToken() throws Exception
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        assertThat(refreshTokenRepository.findActiveTokensByUser(candidate.getId(), Instant.now()))
                .as("active session before the change")
                .isNotEmpty();

        mockMvc.perform(withCsrf(put("/api/users/me/password"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "%s",
                                  "newPassword": "BrandNewPassword123!",
                                  "confirmNewPassword": "BrandNewPassword123!"
                                }
                                """.formatted(RAW_PASSWORD)))
                .andExpect(status().isNoContent());

        assertThat(refreshTokenRepository.findActiveTokensByUser(candidate.getId(), Instant.now()))
                .as("sessions surviving the password change")
                .isEmpty();
    }
}
