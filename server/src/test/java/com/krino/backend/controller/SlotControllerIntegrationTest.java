package com.krino.backend.controller;

import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.repository.SlotRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SlotControllerIntegrationTest extends AbstractControllerIntegrationTest
{
    @Autowired
    private SlotRepository slotRepository;

    @BeforeEach
    void cleanDatabase()
    {
        slotRepository.deleteAll();
        userRepository.deleteAll();
    }

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
                                  "interviewDate": "2099-01-01",
                                  "startTime": "09:00:00",
                                  "endTime": "09:45:00"
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
                                  "interviewDate": "2099-01-01",
                                  "startTime": "09:00:00",
                                  "endTime": "09:45:00"
                                }
                                """.formatted(candidate.getPublicId())))
                .andExpect(status().isForbidden());
    }

    private Slot slotFor(User interviewer)
    {
        Slot slot = new Slot();
        slot.setInterviewer(interviewer);
        slot.setInterviewDate(LocalDate.of(2099, 1, 1));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(9, 45));
        return slot;
    }
}
