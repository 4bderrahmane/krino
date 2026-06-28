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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewControllerIntegrationTest extends AbstractControllerIntegrationTest
{
    private static final String OTHER_CANDIDATE_EMAIL = "other-candidate@test.local";

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
        slot.setInterviewDate(LocalDate.of(2999, Month.JANUARY, 1));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(9, 45));
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
