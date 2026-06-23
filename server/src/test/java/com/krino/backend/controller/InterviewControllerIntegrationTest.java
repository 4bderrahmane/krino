package com.krino.backend.controller;

import com.krino.backend.entity.Department;
import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.JobStatus;
import com.krino.backend.entity.enums.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewControllerIntegrationTest extends AbstractControllerIntegrationTest
{
    @Test
    void adminBooksInterviewIntoFreeSlotReturnsCreatedAndPersists() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Job job = openJob();
        Slot slot = slotRepository.save(slotFor(interviewer));
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        mockMvc.perform(withCsrf(post("/api/interviews"))
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(candidate, job, slot)))
                .andExpect(status().isCreated());

        assertThat(interviewRepository.count()).isEqualTo(1);
    }

    @Test
    void bookingAnAlreadyBookedSlotReturnsConflict() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User candidate = createUser(CANDIDATE_EMAIL, true, UserRole.CANDIDATE);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Job job = openJob();
        Slot slot = slotRepository.save(slotFor(interviewer));
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        String body = bookingBody(candidate, job, slot);

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
    void bookingWithUnknownCandidateReturnsNotFound() throws Exception
    {
        createUser(ADMIN_EMAIL, true, UserRole.ADMIN);
        User interviewer = createUser(INTERVIEWER_EMAIL, true, UserRole.INTERVIEWER);
        Job job = openJob();
        Slot slot = slotRepository.save(slotFor(interviewer));
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);

        String body = """
                {
                  "candidateId": "%s",
                  "jobId": "%s",
                  "slotId": "%s"
                }
                """.formatted(UUID.randomUUID(), job.getPublicId(), slot.getPublicId());

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
        Slot slot = slotRepository.save(slotFor(interviewer));
        Cookie accessCookie = loginAndGetAccessCookie(ADMIN_EMAIL);
        Interview interview = interviewRepository.save(interviewFor(interviewer, candidate, job, slot));

        MvcResult result = mockMvc.perform(get("/api/interviews/" + interview.getPublicId())
                        .cookie(accessCookie))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains(
                "\"id\":\"" + interview.getPublicId() + "\"",
                "\"title\":\"Backend Engineer\"",
                "\"email\":\"" + CANDIDATE_EMAIL + "\""
        );
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

    private String bookingBody(User candidate, Job job, Slot slot)
    {
        return """
                {
                  "candidateId": "%s",
                  "jobId": "%s",
                  "slotId": "%s",
                  "notes": "Intro call",
                  "isOnline": true,
                  "meetingUrl": "https://meet.example/abc"
                }
                """.formatted(candidate.getPublicId(), job.getPublicId(), slot.getPublicId());
    }

    private Job openJob()
    {
        Department department = departmentRepository.save(department());

        Job job = new Job();
        job.setDepartment(department);
        job.setTitle("Backend Engineer");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setContractType(ContractType.PERMANENT);
        job.setStatus(JobStatus.OPEN);
        job.setApplyingDeadline(LocalDate.now().plusDays(30));
        return jobRepository.save(job);
    }

    private Department department()
    {
        Department department = new Department();
        department.setName("Engineering");
        return department;
    }

    private Slot slotFor(User interviewer)
    {
        Slot slot = new Slot();
        slot.setInterviewer(interviewer);
        slot.setInterviewDate(LocalDate.now().plusDays(1));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(9, 45));
        return slot;
    }

    private Interview interviewFor(User interviewer, User candidate, Job job, Slot slot)
    {
        Interview interview = new Interview();
        interview.setInterviewer(interviewer);
        interview.setCandidate(candidate);
        interview.setJob(job);
        interview.setSlot(slot);
        return interview;
    }
}
