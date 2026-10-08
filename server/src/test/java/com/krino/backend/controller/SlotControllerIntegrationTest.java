package com.krino.backend.controller;

import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SlotControllerIntegrationTest extends AbstractControllerIntegrationTest
{
    private static final String OTHER_INTERVIEWER_EMAIL = "other-interviewer@test.local";

    @Test
    void adminCreatesSlotForInterviewerReturnsCreatedAndPersists() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(withCsrf(post("/api/slots"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "interviewerId": "%s",
                                  "startAt": "2099-01-01T09:00:00Z",
                                  "endAt": "2099-01-01T09:45:00Z"
                                }
                                """.formatted(interviewer.getPublicId())))
                .andExpect(status().isCreated());

        assertThat(slotRepository.count()).isEqualTo(1);
    }

    @Test
    void adminReadsSlotByPublicIdIncludingInterviewer() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        Slot slot = slotRepository.save(slotFor(interviewer));

        MvcResult result = mockMvc.perform(get("/api/slots/" + slot.getPublicId())
                        .cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"id\":\"" + slot.getPublicId() + "\"",
                "\"durationInMinutes\":45",
                "\"email\":\"" + INTERVIEWER_EMAIL + "\""
        );
    }

    @Test
    void readingUnknownSlotReturnsNotFound() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(get("/api/slots/" + UUID.randomUUID())
                        .cookie(accessCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminDeletesSlotReturnsNoContent() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        Slot slot = slotRepository.save(slotFor(interviewer));

        mockMvc.perform(withCsrf(delete("/api/slots/" + slot.getPublicId()))
                        .cookie(accessCookie))
                .andExpect(status().isNoContent());

        assertThat(slotRepository.count()).isZero();
    }

    @Test
    void requestWithoutAuthenticationReturnsUnauthorized() throws Exception
    {
        mockMvc.perform(get("/api/slots"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void candidateCannotCreateSlot() throws Exception
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(withCsrf(post("/api/slots"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "interviewerId": "%s",
                                  "startAt": "2099-01-01T09:00:00Z",
                                  "endAt": "2099-01-01T09:45:00Z"
                                }
                                """.formatted(candidate.getPublicId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void interviewerCannotCreateSlotForAnotherInterviewer() throws Exception
    {
        createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        User otherInterviewer = createUser(OTHER_INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(INTERVIEWER_EMAIL);

        mockMvc.perform(withCsrf(post("/api/slots"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "interviewerId": "%s",
                                  "startAt": "2099-01-01T09:00:00Z",
                                  "endAt": "2099-01-01T09:45:00Z"
                                }
                                """.formatted(otherInterviewer.getPublicId())))
                .andExpect(status().isForbidden());

        assertThat(slotRepository.count()).isZero();
    }

    @Test
    void creatingSlotWithEndBeforeStartReturnsBadRequest() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(withCsrf(post("/api/slots"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "interviewerId": "%s",
                                  "startAt": "2099-01-01T10:00:00Z",
                                  "endAt": "2099-01-01T09:00:00Z"
                                }
                                """.formatted(interviewer.getPublicId())))
                .andExpect(status().isBadRequest());

        assertThat(slotRepository.count()).isZero();
    }

    @Test
    void persistingSlotWithInvertedWindowIsRejectedByCheckConstraint()
    {
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Slot slot = slotFor(interviewer);
        slot.setEndAt(Instant.parse("2099-01-01T08:00:00Z")); // earlier than the 09:00 start

        assertThatThrownBy(() -> slotRepository.saveAndFlush(slot))
                .hasStackTraceContaining("ck_slots_time_range");
    }

    @Test
    void persistingSlotWithMissingEndIsRejectedByNotNullConstraint()
    {
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Slot slot = slotFor(interviewer);
        slot.setEndAt(null);

        assertThatThrownBy(() -> slotRepository.saveAndFlush(slot))
                .hasStackTraceContaining("end_at");
    }

    private Slot slotFor(User interviewer)
    {
        Slot slot = new Slot();
        slot.setInterviewer(interviewer);
        slot.setStartAt(Instant.parse("2099-01-01T09:00:00Z"));
        slot.setEndAt(Instant.parse("2099-01-01T09:45:00Z"));
        return slot;
    }
}
