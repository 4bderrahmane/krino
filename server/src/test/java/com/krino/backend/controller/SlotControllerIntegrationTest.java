package com.krino.backend.controller;

import com.krino.backend.entity.Application;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.ApplicationStatus;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.support.TestJobs;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SlotControllerIntegrationTest extends AbstractControllerIntegrationTest
{
    private static final String OTHER_INTERVIEWER_EMAIL = "other-interviewer@test.local";
    private static final Instant START = Instant.parse("2099-01-01T09:00:00Z");
    private static final Instant END = Instant.parse("2099-01-01T09:45:00Z");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void adminCreatesSlotForInterviewerReturnsCreatedAndPersists() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(createSlotRequest(accessCookie, interviewer, "2099-01-01T09:00:00Z", "2099-01-01T09:45:00Z"))
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
                // The window comes back as an absolute instant in UTC, whatever offset it went in as.
                "\"startAt\":\"2099-01-01T09:00:00Z\"",
                "\"endAt\":\"2099-01-01T09:45:00Z\"",
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

        mockMvc.perform(createSlotRequest(accessCookie, candidate, "2099-01-01T09:00:00Z", "2099-01-01T09:45:00Z"))
                .andExpect(status().isForbidden());
    }

    @Test
    void interviewerCannotCreateSlotForAnotherInterviewer() throws Exception
    {
        createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        User otherInterviewer = createUser(OTHER_INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(INTERVIEWER_EMAIL);

        mockMvc.perform(createSlotRequest(accessCookie, otherInterviewer, "2099-01-01T09:00:00Z", "2099-01-01T09:45:00Z"))
                .andExpect(status().isForbidden());

        assertThat(slotRepository.count()).isZero();
    }

    @Test
    void creatingSlotWithEndBeforeStartReturnsBadRequest() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(createSlotRequest(accessCookie, interviewer, "2099-01-01T10:00:00Z", "2099-01-01T09:00:00Z"))
                .andExpect(status().isBadRequest());

        assertThat(slotRepository.count()).isZero();
    }

    @Test
    void creatingSlotInThePastReturnsBadRequest() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(createSlotRequest(accessCookie, interviewer, "2020-01-01T09:00:00Z", "2099-01-01T09:45:00Z"))
                .andExpect(status().isBadRequest());

        assertThat(slotRepository.count()).isZero();
    }

    @Test
    void replacingSlotWithPastStartReturnsBadRequestAndLeavesWindowUntouched() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        Slot slot = slotRepository.save(slotFor(interviewer));

        mockMvc.perform(withCsrf(put("/api/slots/" + slot.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startAt":"2020-01-01T09:00:00Z","endAt":"2099-01-01T09:45:00Z"}
                                """))
                .andExpect(status().isBadRequest());

        Slot unchanged = slotRepository.findById(slot.getId()).orElseThrow();
        assertThat(unchanged.getStartAt()).isEqualTo(START);
        assertThat(unchanged.getEndAt()).isEqualTo(END);
    }

    @Test
    void creatingSlotForUserWithoutInterviewerRoleReturnsConflict() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(createSlotRequest(accessCookie, candidate, "2099-01-01T09:00:00Z", "2099-01-01T09:45:00Z"))
                .andExpect(status().isConflict());

        assertThat(slotRepository.count()).isZero();
    }

    @Test
    void creatingSlotForDeactivatedInterviewerReturnsConflict() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, false, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(createSlotRequest(accessCookie, interviewer, "2099-01-01T09:00:00Z", "2099-01-01T09:45:00Z"))
                .andExpect(status().isConflict());

        assertThat(slotRepository.count()).isZero();
    }

    // The wire format carries an offset, so the same moment written two different ways is
    // still the same moment: 10:00+01:00 is the 09:00Z slot that already exists.
    @Test
    void creatingSlotOverlappingAnExistingOneReturnsConflict() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        slotRepository.save(slotFor(interviewer));

        mockMvc.perform(createSlotRequest(accessCookie, interviewer, "2099-01-01T10:00:00+01:00", "2099-01-01T10:30:00+01:00"))
                .andExpect(status().isConflict());

        assertThat(slotRepository.count()).isEqualTo(1);
    }

    // Half-open windows: a slot that starts exactly when another ends is not an overlap.
    @Test
    void creatingBackToBackSlotIsAccepted() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        slotRepository.save(slotFor(interviewer));

        mockMvc.perform(createSlotRequest(accessCookie, interviewer, "2099-01-01T09:45:00Z", "2099-01-01T10:30:00Z"))
                .andExpect(status().isCreated());

        assertThat(slotRepository.count()).isEqualTo(2);
    }

    // Two interviewers are free to publish the same window.
    @Test
    void creatingSameWindowForAnotherInterviewerIsAccepted() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        User otherInterviewer = createUser(OTHER_INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        slotRepository.save(slotFor(interviewer));

        mockMvc.perform(createSlotRequest(accessCookie, otherInterviewer, "2099-01-01T09:00:00Z", "2099-01-01T09:45:00Z"))
                .andExpect(status().isCreated());

        assertThat(slotRepository.count()).isEqualTo(2);
    }

    @Test
    void patchingSlotOntoAnotherSlotsWindowReturnsConflict() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        slotRepository.save(slotFor(interviewer));
        Slot later = slotRepository.save(slotFor(interviewer, Instant.parse("2099-01-01T11:00:00Z"),
                Instant.parse("2099-01-01T11:45:00Z")));

        mockMvc.perform(withCsrf(patch("/api/slots/" + later.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2099-01-01T09:30:00Z",
                                  "endAt": "2099-01-01T10:15:00Z"
                                }
                                """))
                .andExpect(status().isConflict());

        assertThat(slotRepository.findByPublicId(later.getPublicId()))
                .get()
                .extracting(Slot::getStartAt)
                .isEqualTo(Instant.parse("2099-01-01T11:00:00Z"));
    }

    @Test
    void patchingOnlyEndIntoAnotherSlotsWindowReturnsConflictAndRollsBack() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        Slot slot = slotRepository.save(slotFor(interviewer));
        slotRepository.save(slotFor(interviewer, Instant.parse("2099-01-01T11:00:00Z"),
                Instant.parse("2099-01-01T11:45:00Z")));

        mockMvc.perform(withCsrf(patch("/api/slots/" + slot.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endAt":"2099-01-01T11:15:00Z"}
                                """))
                .andExpect(status().isConflict());

        Slot unchanged = slotRepository.findById(slot.getId()).orElseThrow();
        assertThat(unchanged.getStartAt()).isEqualTo(START);
        assertThat(unchanged.getEndAt()).isEqualTo(END);
        assertThat(slotRepository.count()).isEqualTo(2);
    }

    @Test
    void replacingSlotOntoAnotherSlotsWindowReturnsConflictAndRollsBack() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        slotRepository.save(slotFor(interviewer));
        Instant laterStart = Instant.parse("2099-01-01T11:00:00Z");
        Instant laterEnd = Instant.parse("2099-01-01T11:45:00Z");
        Slot later = slotRepository.save(slotFor(interviewer, laterStart, laterEnd));

        mockMvc.perform(withCsrf(put("/api/slots/" + later.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startAt":"2099-01-01T09:30:00Z","endAt":"2099-01-01T10:15:00Z"}
                                """))
                .andExpect(status().isConflict());

        Slot unchanged = slotRepository.findById(later.getId()).orElseThrow();
        assertThat(unchanged.getStartAt()).isEqualTo(laterStart);
        assertThat(unchanged.getEndAt()).isEqualTo(laterEnd);
        assertThat(slotRepository.count()).isEqualTo(2);
    }

    // Moving a slot onto its own window is not a conflict with itself.
    @Test
    void patchingSlotWithinItsOwnWindowIsAccepted() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        Slot slot = slotRepository.save(slotFor(interviewer));

        mockMvc.perform(withCsrf(patch("/api/slots/" + slot.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endAt": "2099-01-01T09:30:00Z"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(slotRepository.findByPublicId(slot.getPublicId()))
                .get()
                .extracting(Slot::getEndAt)
                .isEqualTo(Instant.parse("2099-01-01T09:30:00Z"));
    }

    @Test
    void patchingBookedSlotReturnsConflictAndLeavesWindowUntouched() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        Slot slot = bookedSlotFor(interviewer);

        mockMvc.perform(withCsrf(patch("/api/slots/" + slot.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2099-02-01T09:00:00Z",
                                  "endAt": "2099-02-01T09:45:00Z"
                                }
                                """))
                .andExpect(status().isConflict());

        assertThat(slotRepository.findByPublicId(slot.getPublicId()))
                .get()
                .extracting(Slot::getStartAt)
                .isEqualTo(START);
    }

    @Test
    void persistingSlotWithInvertedWindowIsRejectedByCheckConstraint()
    {
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Slot slot = slotFor(interviewer);
        slot.setEndAt(START.minusSeconds(3600)); // earlier than the 09:00 start

        assertThatThrownBy(() -> slotRepository.saveAndFlush(slot))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("ck_slots_time_range");
    }

    @Test
    void persistingSlotWithEmptyWindowIsRejectedByCheckConstraint()
    {
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Slot slot = slotFor(interviewer);
        slot.setEndAt(START);

        assertThatThrownBy(() -> slotRepository.saveAndFlush(slot))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("ck_slots_time_range");
    }

    @Test
    void clearingSlotEndIsRejectedByDatabaseNotNullConstraint()
    {
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Slot slot = slotRepository.saveAndFlush(slotFor(interviewer));

        // Use SQL to reach PostgreSQL's constraint without Hibernate's nullability check.
        assertThatThrownBy(() -> jdbc.update("UPDATE slots SET end_at = NULL WHERE id = ?", slot.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("end_at")
                .hasMessageContaining("not-null constraint");

        assertThat(slotRepository.findById(slot.getId()).orElseThrow().getEndAt()).isEqualTo(END);
    }

    // Direct repository writes verify that PostgreSQL enforces the overlap rule.
    @Test
    void persistingOverlappingSlotIsRejectedByExclusionConstraint()
    {
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        slotRepository.saveAndFlush(slotFor(interviewer));

        Slot overlapping = slotFor(interviewer, START.plusSeconds(600), END.plusSeconds(600));

        assertThatThrownBy(() -> slotRepository.saveAndFlush(overlapping))
                .hasStackTraceContaining("ex_slots_interviewer_no_overlap");
    }

    private MockHttpServletRequestBuilder createSlotRequest(
            Cookie accessCookie, User interviewer, String startAt, String endAt) throws Exception
    {
        return withCsrf(post("/api/slots"))
                .cookie(accessCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "interviewerId": "%s",
                          "startAt": "%s",
                          "endAt": "%s"
                        }
                        """.formatted(interviewer.getPublicId(), startAt, endAt));
    }

    private Slot slotFor(User interviewer)
    {
        return slotFor(interviewer, START, END);
    }

    private Slot slotFor(User interviewer, Instant startAt, Instant endAt)
    {
        Slot slot = new Slot();
        slot.setInterviewer(interviewer);
        slot.setStartAt(startAt);
        slot.setEndAt(endAt);
        return slot;
    }

    private Slot bookedSlotFor(User interviewer)
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        Department department = new Department();
        department.setName("Engineering");
        Job job = jobRepository.save(TestJobs.open(departmentRepository.save(department), "Backend Engineer"));

        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        applicationRepository.save(application);

        Slot slot = slotRepository.save(slotFor(interviewer));

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setInterviewer(interviewer);
        interview.setSlot(slot);
        interviewRepository.save(interview);

        return slotRepository.save(slot);
    }
}
