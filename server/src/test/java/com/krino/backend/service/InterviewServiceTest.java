package com.krino.backend.service;

import com.krino.backend.dto.interview.InterviewRequestDTO;
import com.krino.backend.dto.interview.InterviewResponseDTO;
import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.InterviewMapper;
import com.krino.backend.repository.InterviewRepository;
import com.krino.backend.repository.JobRepository;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewServiceTest
{
    private InterviewRepository interviewRepository;
    private UserRepository userRepository;
    private SlotRepository slotRepository;
    private JobRepository jobRepository;
    private InterviewMapper interviewMapper;
    private InterviewService interviewService;

    @BeforeEach
    void setUp()
    {
        interviewRepository = mock(InterviewRepository.class);
        userRepository = mock(UserRepository.class);
        slotRepository = mock(SlotRepository.class);
        jobRepository = mock(JobRepository.class);
        interviewMapper = mock(InterviewMapper.class);
        interviewService = new InterviewService(interviewRepository, userRepository, slotRepository, jobRepository,
                interviewMapper);
    }

    @Test
    void createInterview_freeSlot_booksInterviewerFromSlotAndSaves()
    {
        UUID candidateId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        User candidate = user(candidateId);
        Job job = job(jobId);
        User interviewer = new User();
        Slot slot = freeSlot(interviewer);

        InterviewRequestDTO dto = request(candidateId, jobId, slotId);
        Interview entity = new Interview();
        Interview saved = new Interview();
        InterviewResponseDTO response = new InterviewResponseDTO();

        when(userRepository.findByPublicId(candidateId)).thenReturn(Optional.of(candidate));
        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.of(job));
        when(slotRepository.findByPublicId(slotId)).thenReturn(Optional.of(slot));
        when(interviewMapper.toEntity(dto, interviewer, candidate, job, slot)).thenReturn(entity);
        when(interviewRepository.save(entity)).thenReturn(saved);
        when(interviewMapper.toResponse(saved)).thenReturn(response);

        InterviewResponseDTO result = interviewService.createInterview(dto);

        assertThat(result).isSameAs(response);
        verify(interviewRepository).save(entity);
    }

    @Test
    void createInterview_unknownCandidate_throwsResourceNotFound()
    {
        UUID candidateId = UUID.randomUUID();
        when(userRepository.findByPublicId(candidateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.createInterview(request(candidateId, UUID.randomUUID(),
                UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRepository, never()).save(any());
    }

    @Test
    void createInterview_unknownJob_throwsResourceNotFound()
    {
        UUID candidateId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(userRepository.findByPublicId(candidateId)).thenReturn(Optional.of(user(candidateId)));
        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.createInterview(request(candidateId, jobId, UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRepository, never()).save(any());
    }

    @Test
    void createInterview_unknownSlot_throwsResourceNotFound()
    {
        UUID candidateId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        when(userRepository.findByPublicId(candidateId)).thenReturn(Optional.of(user(candidateId)));
        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.of(job(jobId)));
        when(slotRepository.findByPublicId(slotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.createInterview(request(candidateId, jobId, slotId)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRepository, never()).save(any());
    }

    @Test
    void createInterview_slotAlreadyBooked_throwsConflict()
    {
        UUID candidateId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        Slot slot = freeSlot(new User());
        slot.setInterview(new Interview()); // slot now booked

        when(userRepository.findByPublicId(candidateId)).thenReturn(Optional.of(user(candidateId)));
        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.of(job(jobId)));
        when(slotRepository.findByPublicId(slotId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> interviewService.createInterview(request(candidateId, jobId, slotId)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already booked");

        verify(interviewRepository, never()).save(any());
    }

    @Test
    void getInterviewByPublicId_unknown_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(interviewRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.getInterviewByPublicId(publicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateInterview_unknown_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(interviewRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.updateInterview(publicId, request(UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRepository, never()).save(any());
    }

    @Test
    void updateInterview_slotBookedByDifferentInterview_throwsConflict()
    {
        UUID publicId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        Interview existing = new Interview();
        existing.setId(1L);

        Interview otherBooking = new Interview();
        otherBooking.setId(99L);
        Slot slot = freeSlot(new User());
        slot.setInterview(otherBooking);

        when(interviewRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(userRepository.findByPublicId(candidateId)).thenReturn(Optional.of(user(candidateId)));
        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.of(job(jobId)));
        when(slotRepository.findByPublicId(slotId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> interviewService.updateInterview(publicId, request(candidateId, jobId, slotId)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already booked");

        verify(interviewRepository, never()).save(any());
    }

    @Test
    void patchInterview_withoutOptionalIds_reusesExistingAssociations()
    {
        UUID publicId = UUID.randomUUID();

        User interviewer = new User();
        Slot slot = freeSlot(interviewer);
        User existingCandidate = new User();
        Job existingJob = new Job();

        Interview existing = new Interview();
        existing.setId(1L);
        existing.setCandidate(existingCandidate);
        existing.setJob(existingJob);
        existing.setSlot(slot); // also wires slot.interview = existing

        InterviewRequestDTO dto = new InterviewRequestDTO(); // all ids null

        when(interviewRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(interviewRepository.save(existing)).thenReturn(existing);
        when(interviewMapper.toResponse(existing)).thenReturn(new InterviewResponseDTO());

        interviewService.patchInterview(publicId, dto);

        verify(userRepository, never()).findByPublicId(any());
        verify(jobRepository, never()).findByPublicId(any());
        verify(slotRepository, never()).findByPublicId(any());
        verify(interviewMapper).patchEntity(dto, interviewer, existingCandidate, existingJob, slot, existing);
        verify(interviewRepository).save(existing);
    }

    @Test
    void deleteInterview_unknown_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(interviewRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.deleteInterview(publicId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRepository, never()).delete(any());
    }

    @Test
    void deleteInterview_found_freesSlotAndDeletes()
    {
        UUID publicId = UUID.randomUUID();
        Slot slot = freeSlot(new User());
        Interview existing = new Interview();
        existing.setId(1L);
        existing.setSlot(slot);

        when(interviewRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));

        interviewService.deleteInterview(publicId);

        assertThat(existing.getSlot()).isNull();
        assertThat(slot.getInterview()).isNull();
        verify(interviewRepository).delete(existing);
    }

    private InterviewRequestDTO request(UUID candidateId, UUID jobId, UUID slotId)
    {
        InterviewRequestDTO dto = new InterviewRequestDTO();
        dto.setCandidateId(candidateId);
        dto.setJobId(jobId);
        dto.setSlotId(slotId);
        return dto;
    }

    private User user(UUID publicId)
    {
        User user = new User();
        user.setPublicId(publicId);
        return user;
    }

    private Job job(UUID publicId)
    {
        Job job = new Job();
        job.setPublicId(publicId);
        return job;
    }

    private Slot freeSlot(User interviewer)
    {
        Slot slot = new Slot();
        slot.setInterviewer(interviewer);
        return slot;
    }
}
