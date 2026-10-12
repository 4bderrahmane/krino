package com.krino.backend.controller;

import com.krino.backend.entity.*;
import com.krino.backend.entity.enums.*;
import com.krino.backend.support.TestJobs;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InterviewLifecycleIntegrationTest extends AbstractControllerIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    private Cookie adminCookie;
    private User interviewer;
    private Application application;
    private Slot slot;

    @BeforeEach
    void setUpScenario() throws Exception {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Department department = new Department();
        department.setName("Engineering");
        department = departmentRepository.save(department);
        Job job = jobRepository.save(TestJobs.open(department, "Backend Engineer"));
        application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);
        application = applicationRepository.save(application);
        slot = newSlot(Instant.parse("2999-01-01T09:00:00Z"));
        adminCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PATCH", "PUT"})
    void cancellationReleasesSlotPreservesRecordAndAllowsRebooking(String method) throws Exception {
        UUID id = book(slot);
        cancel(id, method);
        assertThat(interviewRepository.findByPublicId(id).orElseThrow().getStatus()).isEqualTo(InterviewStatus.CANCELLED);
        assertThat(slotRepository.findById(slot.getId()).orElseThrow().isAvailable()).isTrue();
        assertThat(currentApplicationStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        mockMvc.perform(get("/api/interviews/" + id).cookie(adminCookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.slot").isEmpty())
                .andExpect(jsonPath("$.notes").value("Candidate requested cancellation"));
        UUID replacement = book(slot);
        assertThat(replacement).isNotEqualTo(id);
        assertThat(interviewRepository.count()).isEqualTo(2);
        assertThat(currentApplicationStatus()).isEqualTo(ApplicationStatus.INTERVIEW_SCHEDULED);
        // Saving the cancelled record again must not take the now-rebooked slot back.
        cancel(id, method);
        assertThat(slotRepository.findById(slot.getId()).orElseThrow().isAvailable()).isFalse();
        assertThat(currentApplicationStatus()).isEqualTo(ApplicationStatus.INTERVIEW_SCHEDULED);
    }

    @Test
    void onlyCancellingTheLastScheduledRoundReturnsApplicationToReview() throws Exception {
        UUID first = book(slot);
        UUID second = book(newSlot(Instant.parse("2999-01-02T09:00:00Z")));
        cancel(first, "PATCH");
        assertThat(currentApplicationStatus()).isEqualTo(ApplicationStatus.INTERVIEW_SCHEDULED);
        cancel(second, "PUT");
        assertThat(currentApplicationStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACCEPTED", "REJECTED"})
    void cancellationPreservesHiringDecision(String decision) throws Exception {
        UUID id = book(slot);
        Application current = applicationRepository.findById(application.getId()).orElseThrow();
        current.setStatus(ApplicationStatus.valueOf(decision));
        applicationRepository.save(current);
        cancel(id, "PATCH");
        assertThat(currentApplicationStatus()).isEqualTo(ApplicationStatus.valueOf(decision));
    }

    @Test
    void deletingLastInterviewAlsoReleasesSlotAndReturnsApplicationToReview() throws Exception {
        UUID id = book(slot);
        mockMvc.perform(withCsrf(delete("/api/interviews/" + id)).cookie(adminCookie))
                .andExpect(status().isNoContent());
        assertThat(currentApplicationStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(slotRepository.findById(slot.getId()).orElseThrow().isAvailable()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"past", "deactivated", "wrong-role"})
    void rejectsNewBookingWhenPublishedSlotIsNoLongerEligible(String reason) throws Exception {
        makeIneligible(slot, reason);
        mockMvc.perform(withCsrf(post("/api/interviews")).cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingBody(slot)))
                .andExpect(status().isConflict());
        assertThat(interviewRepository.count()).isZero();
        assertThat(currentApplicationStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(slotRepository.findById(slot.getId()).orElseThrow().isAvailable()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PATCH", "PUT"})
    void rejectsReschedulingIntoPastSlotWithoutLosingOriginalBooking(String method) throws Exception {
        UUID id = book(slot);
        Slot past = newSlot(Instant.parse("2000-01-01T09:00:00Z"));
        mockMvc.perform(withCsrf(update(method, id)).cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingBody(past)))
                .andExpect(status().isConflict());
        assertThat(interviewRepository.findByPublicId(id).orElseThrow().getSlot().getPublicId()).isEqualTo(slot.getPublicId());
        assertThat(slotRepository.findById(slot.getId()).orElseThrow().isAvailable()).isFalse();
    }

    @Test
    void rejectsReschedulingIntoDeactivatedInterviewersSlot() throws Exception {
        UUID id = book(slot);
        Slot next = newSlot(Instant.parse("2999-01-02T09:00:00Z"));
        makeIneligible(next, "deactivated");
        mockMvc.perform(withCsrf(patch("/api/interviews/" + id)).cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingBody(next)))
                .andExpect(status().isConflict());
        assertThat(interviewRepository.findByPublicId(id).orElseThrow().getSlot().getPublicId()).isEqualTo(slot.getPublicId());
    }

    @Test
    void cancelledInterviewNeedsEligibleSlotToBeReopened() throws Exception {
        UUID id = book(slot);
        cancel(id, "PATCH");
        mockMvc.perform(withCsrf(patch("/api/interviews/" + id)).cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SCHEDULED\"}"))
                .andExpect(status().isBadRequest());
        makeIneligible(slot, "past");
        mockMvc.perform(withCsrf(patch("/api/interviews/" + id)).cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingBody(slot)))
                .andExpect(status().isConflict());
        Slot future = newSlot(Instant.parse("2999-01-02T09:00:00Z"));
        mockMvc.perform(withCsrf(put("/api/interviews/" + id)).cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingBody(future)))
                .andExpect(status().isOk());
        assertThat(currentApplicationStatus()).isEqualTo(ApplicationStatus.INTERVIEW_SCHEDULED);
    }

    @Test
    void existingPastInterviewCanBeCompletedButNotReopenedInThePast() throws Exception {
        UUID id = book(slot);
        makeIneligible(slot, "past");
        makeIneligible(slot, "deactivated");
        mockMvc.perform(withCsrf(patch("/api/interviews/" + id)).cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\",\"recommendation\":\"YES\",\"notes\":\"Good\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(withCsrf(put("/api/interviews/" + id)).cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingBody(slot)))
                .andExpect(status().isConflict());
    }

    @Test
    void existingPastInterviewCanStillBeCancelledByItsInterviewer() throws Exception {
        UUID id = book(slot);
        makeIneligible(slot, "past");
        Cookie owner = loginAndGetAccessCookie(INTERVIEWER_EMAIL);
        mockMvc.perform(withCsrf(patch("/api/interviews/" + id)).cookie(owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());
        assertThat(slotRepository.findById(slot.getId()).orElseThrow().isAvailable()).isTrue();
        assertThat(currentApplicationStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
    }

    @Test
    void concurrentCancellationsDoNotLeaveApplicationScheduled() throws Exception {
        UUID first = book(slot);
        UUID second = book(newSlot(Instant.parse("2999-01-02T09:00:00Z")));
        // Prepare CSRF exchanges before dispatch; each worker has its own HTTP request.
        var firstRequest = withCsrf(patch("/api/interviews/" + first)).cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}");
        var secondRequest = withCsrf(patch("/api/interviews/" + second)).cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> a = executor.submit(() -> performAfter(start, firstRequest));
            Future<?> b = executor.submit(() -> performAfter(start, secondRequest));
            start.countDown();
            a.get(20, TimeUnit.SECONDS);
            b.get(20, TimeUnit.SECONDS);
        }
        assertThat(currentApplicationStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
    }

    private void performAfter(CountDownLatch start, MockHttpServletRequestBuilder request) {
        try {
            start.await();
            mockMvc.perform(request).andExpect(status().isOk());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Slot newSlot(Instant start) {
        Slot next = new Slot();
        next.setInterviewer(interviewer);
        next.setStartAt(start);
        next.setEndAt(start.plusSeconds(2700));
        return slotRepository.save(next);
    }

    private void makeIneligible(Slot target, String reason) {
        if (reason.equals("past")) {
            // Simulate time passing after a legitimate booking, without invoking slot-edit rules.
            jdbc.update("UPDATE slots SET start_at = ?, end_at = ? WHERE id = ?",
                    java.sql.Timestamp.from(Instant.parse("2000-01-01T09:00:00Z")),
                    java.sql.Timestamp.from(Instant.parse("2000-01-01T09:45:00Z")), target.getId());
        } else {
            User current = userRepository.findById(interviewer.getId()).orElseThrow();
            if (reason.equals("deactivated")) current.setApproved(false);
            else current.setRoles(java.util.Set.of(UserRole.HR_MANAGER));
            userRepository.save(current);
        }
    }

    private UUID book(Slot chosen) throws Exception {
        var result = mockMvc.perform(withCsrf(post("/api/interviews")).cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingBody(chosen)))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private void cancel(UUID id, String method) throws Exception {
        String body = """
                {"applicationId":"%s","status":"CANCELLED","isOnline":false,"notes":"Candidate requested cancellation"}
                """.formatted(application.getPublicId());
        mockMvc.perform(withCsrf(update(method, id)).cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder update(String method, UUID id) {
        return method.equals("PUT") ? put("/api/interviews/" + id) : patch("/api/interviews/" + id);
    }

    private String bookingBody(Slot chosen) {
        return """
                {"applicationId":"%s","slotId":"%s","isOnline":false,"status":"SCHEDULED"}
                """.formatted(application.getPublicId(), chosen.getPublicId());
    }

    private ApplicationStatus currentApplicationStatus() {
        return applicationRepository.findById(application.getId()).orElseThrow().getStatus();
    }
}
