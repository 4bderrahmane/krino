package com.krino.backend.controller;

import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PasswordChangeRequiredIntegrationTest extends AbstractControllerIntegrationTest {

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "HR_MANAGER", "INTERVIEWER"})
    void initialPasswordSessionIsRestrictedUntilPasswordChanges(UserRole role) throws Exception {
        User user = createPendingUser(role);
        Cookie cookie = loginAndGetAccessCookie(user.getEmail());

        mockMvc.perform(get("/api/users/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
        mockMvc.perform(get("/api/jobs").cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("PASSWORD_CHANGE_REQUIRED"));
        mockMvc.perform(withCsrf(patch("/api/users/me")).cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"Changed\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PASSWORD_CHANGE_REQUIRED"));

        // Signing in again with the initial password must not lift the restriction.
        Cookie secondLogin = loginAndGetAccessCookie(user.getEmail());
        mockMvc.perform(get("/api/jobs").cookie(secondLogin))
                .andExpect(status().isForbidden());

        mockMvc.perform(withCsrf(put("/api/users/me/password")).cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content(passwordBody("NewPassword123!")))
                .andExpect(status().isNoContent());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.isMustChangePassword()).isFalse();
        assertThat(passwordEncoder.matches("NewPassword123!", updated.getPassword())).isTrue();
        assertThat(refreshTokenRepository.findActiveTokensByUser(user.getId(), java.time.Instant.now())).isEmpty();
        mockMvc.perform(get("/api/users/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
        mockMvc.perform(get("/api/jobs").cookie(cookie)).andExpect(status().isOk());
    }

    @Test
    void initialPasswordCannotBeReusedToClearTheFlag() throws Exception {
        User user = createPendingUser(UserRole.HR_MANAGER);
        Cookie cookie = loginAndGetAccessCookie(user.getEmail());
        mockMvc.perform(withCsrf(put("/api/users/me/password")).cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content(passwordBody(RAW_PASSWORD)))
                .andExpect(status().isBadRequest());
        assertThat(userRepository.findById(user.getId()).orElseThrow().isMustChangePassword()).isTrue();
    }

    @Test
    void passwordChangeStillRequiresCsrfAndCorrectCurrentPassword() throws Exception {
        User user = createPendingUser(UserRole.INTERVIEWER);
        Cookie cookie = loginAndGetAccessCookie(user.getEmail());
        mockMvc.perform(put("/api/users/me/password").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content(passwordBody("NewPassword123!")))
                .andExpect(status().isForbidden());
        mockMvc.perform(withCsrf(put("/api/users/me/password")).cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("NewPassword123!").replace(RAW_PASSWORD, "WrongPassword123!")))
                .andExpect(status().isUnauthorized());
        assertThat(userRepository.findById(user.getId()).orElseThrow().isMustChangePassword()).isTrue();
    }

    @Test
    void publicCatalogueCsrfAndLogoutRemainAccessible() throws Exception {
        User user = createPendingUser(UserRole.HR_MANAGER);
        Cookie cookie = loginAndGetAccessCookie(user.getEmail());
        mockMvc.perform(get("/api/public/jobs").cookie(cookie)).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/csrf").cookie(cookie)).andExpect(status().isOk());
        mockMvc.perform(withCsrf(post("/api/auth/logout")).cookie(cookie)).andExpect(status().is2xxSuccessful());
    }

    private User createPendingUser(UserRole role) {
        User user = createUser("initial-password@test.local", true, role);
        user.setMustChangePassword(true);
        return userRepository.save(user);
    }

    private String passwordBody(String newPassword) {
        return """
                {"currentPassword":"%s","newPassword":"%s","confirmNewPassword":"%s"}
                """.formatted(RAW_PASSWORD, newPassword, newPassword);
    }
}
