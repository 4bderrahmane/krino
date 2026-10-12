package com.krino.backend.controller;

import com.krino.backend.entity.Application;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.ApplicationStatus;
import com.krino.backend.entity.enums.InterviewRecommendation;
import com.krino.backend.entity.enums.InterviewStatus;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.support.TestJobs;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewControllerIntegrationTest extends AbstractControllerIntegrationTest
{
    private static final String OTHER_CANDIDATE_EMAIL = "other-candidate@test.local";
    private static final String OTHER_INTERVIEWER_EMAIL = "other-interviewer@test.local";

    @Test
    void adminBooksInterviewIntoFreeSlotReturnsCreatedAndPersists() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Job job = openJob();
        Application application = applicationFor(candidate, job);
        Slot slot = slotRepository.save(slotFor(interviewer));
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(withCsrf(post("/api/interviews"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(application, slot)))
                .andExpect(status().isCreated());

        assertThat(interviewRepository.count()).isEqualTo(1);
        assertThat(applicationRepository.findByPublicId(application.getPublicId()).orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.INTERVIEW_SCHEDULED);
    }

    @Test
    void bookingAnAlreadyBookedSlotReturnsConflict() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Job job = openJob();
        Application application = applicationFor(candidate, job);
        Slot slot = slotRepository.save(slotFor(interviewer));
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        String body = bookingBody(application, slot);

        mockMvc.perform(withCsrf(post("/api/interviews"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(withCsrf(post("/api/interviews"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void bookingWithUnknownApplicationReturnsNotFound() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Slot slot = slotRepository.save(slotFor(interviewer));
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        String body = """
                {
                  "applicationId": "%s",
                  "slotId": "%s",
                  "isOnline": false
                }
                """.formatted(UUID.randomUUID(), slot.getPublicId());

        mockMvc.perform(withCsrf(post("/api/interviews"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminReadsInterviewIncludingAssociations() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Job job = openJob();
        Application application = applicationFor(candidate, job);
        Slot slot = slotRepository.save(slotFor(interviewer));
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        Interview interview = interviewRepository.save(interviewFor(interviewer, application, slot));

        MvcResult result = mockMvc.perform(get("/api/interviews/" + interview.getPublicId())
                        .cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"id\":\"" + interview.getPublicId() + "\"",
                "\"applicationId\":\"" + application.getPublicId() + "\"",
                "\"title\":\"Backend Engineer\"",
                "\"email\":\"" + CANDIDATE_EMAIL + "\""
        );
    }

    /**
     * A candidate reading their own interview gets logistics and nothing else. Asserted against the
     * raw response body rather than parsed fields, because the point is what is absent from the
     * wire, not what a deserializer happens to populate.
     */
    @Test
    void candidateReadsOwnInterviewWithoutInternalOrContactFields() throws Exception
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Job job = openJob();
        Application application = applicationFor(candidate, job);
        Slot slot = slotRepository.save(slotFor(interviewer));
        Interview interview = interviewFor(interviewer, application, slot);
        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setNotes("Weak on system design, strong on fundamentals");
        interview.setRecommendation(InterviewRecommendation.NO);
        interviewRepository.save(interview);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        MvcResult result = mockMvc.perform(get("/api/interviews/" + interview.getPublicId())
                        .cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains(
                "\"id\":\"" + interview.getPublicId() + "\"",
                "\"applicationId\":\"" + application.getPublicId() + "\"",
                "\"title\":\"Backend Engineer\"",
                "\"startAt\":\"2999-01-01T09:00:00Z\"");
        assertThat(body).doesNotContain(
                "Weak on system design",
                "\"notes\"",
                "\"recommendation\"",
                INTERVIEWER_EMAIL,
                CANDIDATE_EMAIL);
    }

    /** The interviewer conducting it keeps the full record, notes included. */
    @Test
    void interviewerReadsOwnInterviewWithInternalFields() throws Exception
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Job job = openJob();
        Application application = applicationFor(candidate, job);
        Slot slot = slotRepository.save(slotFor(interviewer));
        Interview interview = interviewFor(interviewer, application, slot);
        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setNotes("Weak on system design, strong on fundamentals");
        interview.setRecommendation(InterviewRecommendation.NO);
        interviewRepository.save(interview);
        Cookie accessCookie = loginAndGetAccessCookie(INTERVIEWER_EMAIL);

        MvcResult result = mockMvc.perform(get("/api/interviews/" + interview.getPublicId())
                        .cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "Weak on system design",
                "\"recommendation\":\"NO\"",
                "\"email\":\"" + CANDIDATE_EMAIL + "\"");
    }

    @Test
    void interviewerListsAndRecordsOutcomeForOwnInterview() throws Exception
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        User other = createUser(OTHER_INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Job job = openJob();
        Application application = applicationFor(candidate, job);
        Interview own = interviewRepository.save(interviewFor(interviewer, application,
                slotRepository.save(slotFor(interviewer))));
        Interview foreign = interviewRepository.save(interviewFor(other, application,
                slotRepository.save(slotFor(other))));
        Cookie accessCookie = loginAndGetAccessCookie(INTERVIEWER_EMAIL);

        MvcResult listing = mockMvc.perform(get("/api/interviews/me").cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(listing.getResponse().getContentAsString())
                .contains(own.getPublicId().toString(), "\"totalElements\":1")
                .doesNotContain(foreign.getPublicId().toString());

        mockMvc.perform(withCsrf(patch("/api/interviews/" + own.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"COMPLETED","recommendation":"YES","notes":"Strong problem solving"}
                                """))
                .andExpect(status().isOk());

        Interview updated = interviewRepository.findByPublicId(own.getPublicId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(updated.getRecommendation()).isEqualTo(InterviewRecommendation.YES);
        assertThat(updated.getNotes()).isEqualTo("Strong problem solving");
    }

    @Test
    void interviewerCannotRecordOutcomeForAnotherInterviewersInterview() throws Exception
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        User other = createUser(OTHER_INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Application application = applicationFor(candidate, openJob());
        Interview foreign = interviewRepository.save(interviewFor(other, application,
                slotRepository.save(slotFor(other))));
        Cookie accessCookie = loginAndGetAccessCookie(INTERVIEWER_EMAIL);

        mockMvc.perform(withCsrf(patch("/api/interviews/" + foreign.getPublicId()))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"COMPLETED","recommendation":"YES","notes":"Unauthorized"}
                                """))
                .andExpect(status().isForbidden());
        assertThat(interviewRepository.findByPublicId(foreign.getPublicId()).orElseThrow().getNotes())
                .isNull();
    }

    /** Same redaction on the list endpoint, which is how a candidate normally arrives. */
    @Test
    void candidateListingOwnInterviewsGetsTheRedactedView() throws Exception
    {
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Job job = openJob();
        Application application = applicationFor(candidate, job);
        Slot slot = slotRepository.save(slotFor(interviewer));
        Interview interview = interviewFor(interviewer, application, slot);
        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setNotes("Weak on system design, strong on fundamentals");
        interview.setRecommendation(InterviewRecommendation.NO);
        interviewRepository.save(interview);
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        MvcResult result = mockMvc.perform(get("/api/interviews/me").cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"id\":\"" + interview.getPublicId() + "\"");
        assertThat(body).doesNotContain(
                "Weak on system design",
                "\"notes\"",
                "\"recommendation\"",
                INTERVIEWER_EMAIL,
                CANDIDATE_EMAIL);
    }

    @Test
    void candidateCannotReadAnotherCandidatesInterview() throws Exception
    {
        createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        User otherCandidate = createUser(OTHER_CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Job job = openJob();
        Application application = applicationFor(otherCandidate, job);
        Slot slot = slotRepository.save(slotFor(interviewer));
        Interview interview = interviewRepository.save(interviewFor(interviewer, application, slot));
        Cookie accessCookie = loginAndGetAccessCookie(CANDIDATE_EMAIL);

        mockMvc.perform(get("/api/interviews/" + interview.getPublicId())
                        .cookie(accessCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void readingUnknownInterviewReturnsNotFound() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(get("/api/interviews/" + UUID.randomUUID())
                        .cookie(accessCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestWithoutAuthenticationReturnsUnauthorized() throws Exception
    {
        mockMvc.perform(get("/api/interviews"))
                .andExpect(status().isUnauthorized());
    }

    private String bookingBody(Application application, Slot slot)
    {
        return """
                {
                  "applicationId": "%s",
                  "slotId": "%s",
                  "notes": "Intro call",
                  "isOnline": true,
                  "meetingUrl": "https://meet.example/abc"
                }
                """.formatted(application.getPublicId(), slot.getPublicId());
    }

    private Job openJob()
    {
        Department department = departmentRepository.save(department());
        return jobRepository.save(TestJobs.open(department, "Backend Engineer"));
    }

    private Department department()
    {
        Department department = new Department();
        department.setName("Engineering");
        return department;
    }

    private Application applicationFor(User candidate, Job job)
    {
        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        return applicationRepository.save(application);
    }

    private Slot slotFor(User interviewer)
    {
        Slot slot = new Slot();
        slot.setInterviewer(interviewer);
        slot.setStartAt(Instant.parse("2999-01-01T09:00:00Z"));
        slot.setEndAt(Instant.parse("2999-01-01T09:45:00Z"));
        return slot;
    }

    private Interview interviewFor(User interviewer, Application application, Slot slot)
    {
        Interview interview = new Interview();
        interview.setInterviewer(interviewer);
        interview.setApplication(application);
        interview.setSlot(slot);
        return interview;
    }
}
