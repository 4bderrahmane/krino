package com.krino.backend.controller;

import com.jayway.jsonpath.JsonPath;
import com.krino.backend.dto.user.UserUpdateDTO;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.mapper.UserMapper;
import com.krino.backend.service.email.EmailDispatcher;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileUpdateIntegrationTest extends AbstractControllerIntegrationTest {
    private static final String NEW_PASSWORD = "BrandNewPassword123!";

    @MockitoBean
    private EmailDispatcher emailDispatcher;

    @MockitoSpyBean
    private UserMapper spiedMapper;

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void profileRejectsBlankEmail(String email) throws Exception {
        User target = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie cookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(patch("/api/users/me"))
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":%d,\"email\":\"%s\"}".formatted(target.getVersion(), email)))
                .andExpect(status().isBadRequest());

        User reloaded = userRepository.findById(target.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo(CANDIDATE_EMAIL);
        assertThat(reloaded.isEmailVerified()).isTrue();
        assertThat(emailVerificationTokenRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void oldProfileFormIsRejectedAfterAnotherUpdate(boolean fullUpdate) throws Exception {
        User target = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie cookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);
        MvcResult snapshot = mockMvc.perform(get("/api/users/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").isNumber())
                .andReturn();
        long originalVersion = ((Number) JsonPath.read(snapshot.getResponse().getContentAsString(), "$.version"))
                .longValue();

        mockMvc.perform(withCsrf(patch("/api/users/me")).cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":%d,\"firstName\":\"New\"}".formatted(originalVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(originalVersion + 1));

        MockHttpServletRequestBuilder staleRequest = fullUpdate
                ? put("/api/users/" + target.getPublicId()).content("""
                        {"version":%d,"firstName":"Test","lastName":"Old","email":"%s","phoneNumber":"123456789"}
                        """.formatted(originalVersion, CANDIDATE_EMAIL))
                : patch("/api/users/" + target.getPublicId()).content("""
                        {"version":%d,"lastName":"Old"}
                        """.formatted(originalVersion));
        mockMvc.perform(withCsrf(staleRequest).cookie(cookie).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STALE_PROFILE"));

        User saved = userRepository.findById(target.getId()).orElseThrow();
        assertThat(saved.getFirstName()).isEqualTo("New");
        assertThat(saved.getLastName()).isEqualTo("User");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void profileUpdateRequiresOriginalVersion(boolean fullUpdate) throws Exception {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie cookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);
        MockHttpServletRequestBuilder request = fullUpdate
                ? put("/api/users/me").content("""
                        {"firstName":"New","lastName":"User","email":"%s","phoneNumber":"123456789"}
                        """.formatted(CANDIDATE_EMAIL))
                : patch("/api/users/me").content("{\"firstName\":\"New\"}");
        mockMvc.perform(withCsrf(request).cookie(cookie).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PATCH", "PUT"})
    void oldProfileFormCannotUndoLaterPasswordChange(String method) throws Exception {
        User target = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie cookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);
        MvcResult snapshot = mockMvc.perform(get("/api/users/me").cookie(cookie))
                .andExpect(status().isOk())
                .andReturn();
        long originalVersion = ((Number) JsonPath.read(snapshot.getResponse().getContentAsString(), "$.version"))
                .longValue();

        mockMvc.perform(withCsrf(put("/api/users/me/password")).cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"%s","confirmNewPassword":"%s"}
                                """.formatted(RAW_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        MockHttpServletRequestBuilder staleRequest = method.equals("PUT")
                ? put("/api/users/me").content("""
                        {"version":%d,"firstName":"Stale","lastName":"User","email":"%s","phoneNumber":"123456789"}
                        """.formatted(originalVersion, CANDIDATE_EMAIL))
                : patch("/api/users/me").content("{\"version\":%d,\"firstName\":\"Stale\"}"
                        .formatted(originalVersion));
        mockMvc.perform(withCsrf(staleRequest).cookie(cookie).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STALE_PROFILE"));

        User saved = userRepository.findById(target.getId()).orElseThrow();
        assertThat(saved.getFirstName()).isEqualTo("Test");
        assertThat(passwordEncoder.matches(NEW_PASSWORD, saved.getPassword())).isTrue();
    }

    @ParameterizedTest
    @EnumSource(AccountChange.class)
    void profileUpdatePreservesConcurrentAccountStateChange(AccountChange change) throws Exception {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User target = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie adminCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        Cookie targetCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        CountDownLatch profileLoaded = new CountDownLatch(1);
        CountDownLatch accountChangeCommitted = new CountDownLatch(1);
        var realMapperAnswer = mockingDetails(spiedMapper).getMockCreationSettings().getDefaultAnswer();
        // Pause after the profile transaction has loaded the user, before it writes anything.
        doAnswer(invocation -> {
            profileLoaded.countDown();
            if (!accountChangeCommitted.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent account change did not finish.");
            }
            return realMapperAnswer.answer(invocation);
        }).when(spiedMapper).patchEntity(any(UserUpdateDTO.class), isNull(), any(User.class));

        MockHttpServletRequestBuilder profileRequest = withCsrf(patch("/api/users/me"))
                .cookie(targetCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":%d,\"firstName\":\"Updated\"}".formatted(target.getVersion()));
        MockHttpServletRequestBuilder accountRequest = change == AccountChange.DEACTIVATE
                ? withCsrf(patch("/api/users/" + target.getPublicId() + "/approval"))
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":false}")
                : withCsrf(put("/api/users/me/password"))
                        .cookie(targetCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "%s",
                                  "newPassword": "%s",
                                  "confirmNewPassword": "%s"
                                }
                                """.formatted(RAW_PASSWORD, NEW_PASSWORD, NEW_PASSWORD));

        MvcResult profileResult;
        var executor = Executors.newSingleThreadExecutor();
        try {
            var profileFuture = executor.submit(() -> mockMvc.perform(profileRequest).andReturn());
            assertThat(profileLoaded.await(15, TimeUnit.SECONDS)).as("profile transaction loaded the user").isTrue();
            mockMvc.perform(accountRequest).andExpect(status().isNoContent());
            assertAccountChangePreserved(change, userRepository.findById(target.getId()).orElseThrow());

            accountChangeCommitted.countDown();
            profileResult = profileFuture.get(30, TimeUnit.SECONDS);
        } finally {
            accountChangeCommitted.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).as("profile worker stopped").isTrue();
        }

        // Either preserve the concurrent change on success or reject the stale profile write.
        int profileStatus = profileResult.getResponse().getStatus();
        assertThat(profileStatus).isIn(200, 409);
        if (profileStatus == 409) {
            assertThat(profileResult.getResolvedException()).isInstanceOf(ConcurrencyFailureException.class);
            jsonPath("$.errorCode").value("DATA_CONFLICT").match(profileResult);
        }

        User afterBothTransactions = userRepository.findById(target.getId()).orElseThrow();
        assertThat(afterBothTransactions.getFirstName()).isEqualTo(profileStatus == 200 ? "Updated" : "Test");
        assertAccountChangePreserved(change, afterBothTransactions);
    }

    private void assertAccountChangePreserved(AccountChange change, User user) {
        if (change == AccountChange.DEACTIVATE) {
            assertThat(user.isApproved()).as("profile edit must preserve deactivation").isFalse();
        } else {
            assertThat(passwordEncoder.matches(NEW_PASSWORD, user.getPassword()))
                    .as("profile edit must preserve new password").isTrue();
            assertThat(passwordEncoder.matches(RAW_PASSWORD, user.getPassword()))
                    .as("old password must remain invalid").isFalse();
        }
    }

    private enum AccountChange {
        DEACTIVATE, PASSWORD
    }
}
