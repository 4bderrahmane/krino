package com.krino.backend.service;

import com.krino.backend.dto.interview.InterviewRequestDTO;
import com.krino.backend.dto.interview.InterviewResponseDTO;
import com.krino.backend.entity.Application;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.ApplicationStatus;
import com.krino.backend.entity.enums.InterviewRecommendation;
import com.krino.backend.entity.enums.InterviewStatus;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.support.TestJobs;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.InterviewMapper;
import com.krino.backend.repository.ApplicationRepository;
import com.krino.backend.repository.InterviewRepository;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
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
    private ApplicationRepository applicationRepository;
    private InterviewMapper interviewMapper;
    private InterviewService interviewService;

    @BeforeEach
    void setUp()
    {
        interviewRepository = mock(InterviewRepository.class);
        userRepository = mock(UserRepository.class);
        slotRepository = mock(SlotRepository.class);
        applicationRepository = mock(ApplicationRepository.class);
        interviewMapper = mock(InterviewMapper.class);
        interviewService = new InterviewService(interviewRepository, userRepository, slotRepository,
                applicationRepository, interviewMapper);
    }

    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createInterview_freeSlot_booksInterviewerFromSlotAndAdvancesApplication()
    {
        authenticateAsAdmin();
        UUID applicationId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        Application application = application(applicationId, ApplicationStatus.UNDER_REVIEW);
        User interviewer = new User();
        Slot slot = freeSlot(interviewer);

        InterviewRequestDTO dto = request(applicationId, slotId);
        Interview entity = new Interview();
        Interview saved = new Interview();
        InterviewResponseDTO response = new InterviewResponseDTO();

        when(applicationRepository.findByPublicId(applicationId)).thenReturn(Optional.of(application));
        when(slotRepository.findByPublicId(slotId)).thenReturn(Optional.of(slot));
        when(interviewMapper.toEntity(dto, interviewer, application, slot)).thenReturn(entity);
        when(interviewRepository.save(entity)).thenReturn(saved);
        when(interviewMapper.toResponse(saved)).thenReturn(response);

        InterviewResponseDTO result = interviewService.createInterview(dto);

        assertThat(result).isSameAs(response);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW_SCHEDULED);
        verify(interviewRepository).save(entity);
    }

    @Test
    void createInterview_unknownApplication_throwsResourceNotFound()
    {
        authenticateAsAdmin();
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findByPublicId(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.createInterview(request(applicationId, UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRepository, never()).save(any());
    }

    @Test
    void createInterview_decidedApplication_throwsConflict()
    {
        authenticateAsAdmin();
        UUID applicationId = UUID.randomUUID();
        Application rejected = application(applicationId, ApplicationStatus.REJECTED);
        when(applicationRepository.findByPublicId(applicationId)).thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> interviewService.createInterview(request(applicationId, UUID.randomUUID())))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("rejected");

        verify(interviewRepository, never()).save(any());
    }

    @Test
    void createInterview_unknownSlot_throwsResourceNotFound()
    {
        authenticateAsAdmin();
        UUID applicationId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        when(applicationRepository.findByPublicId(applicationId))
                .thenReturn(Optional.of(application(applicationId, ApplicationStatus.PENDING)));
        when(slotRepository.findByPublicId(slotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.createInterview(request(applicationId, slotId)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRepository, never()).save(any());
    }

    @Test
    void createInterview_slotAlreadyBooked_throwsConflict()
    {
        authenticateAsAdmin();
        UUID applicationId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        Slot slot = freeSlot(new User());
        slot.setInterview(new Interview()); // slot now booked

        when(applicationRepository.findByPublicId(applicationId))
                .thenReturn(Optional.of(application(applicationId, ApplicationStatus.PENDING)));
        when(slotRepository.findByPublicId(slotId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> interviewService.createInterview(request(applicationId, slotId)))
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

        assertThatThrownBy(() -> interviewService.updateInterview(publicId,
                request(UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRepository, never()).save(any());
    }

    @Test
    void updateInterview_changingApplication_throwsConflict()
    {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();

        Interview existing = new Interview();
        existing.setId(1L);
        existing.setApplication(application(UUID.randomUUID(), ApplicationStatus.INTERVIEW_SCHEDULED));

        InterviewRequestDTO dto = request(UUID.randomUUID(), UUID.randomUUID()); // a different application id

        when(interviewRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> interviewService.updateInterview(publicId, dto))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("application cannot be changed");

        verify(interviewRepository, never()).save(any());
    }

    @Test
    void updateInterview_slotBookedByDifferentInterview_throwsConflict()
    {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        Interview existing = new Interview();
        existing.setId(1L);

        Interview otherBooking = new Interview();
        otherBooking.setId(99L);
        Slot slot = freeSlot(new User());
        slot.setInterview(otherBooking);

        // applicationId omitted (null) so the immutability check is a no-op for this scenario
        InterviewRequestDTO dto = new InterviewRequestDTO();
        dto.setSlotId(slotId);

        when(interviewRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(slotRepository.findByPublicId(slotId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> interviewService.updateInterview(publicId, dto))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already booked");

        verify(interviewRepository, never()).save(any());
    }

    @Test
    void updateInterview_completedWithoutRecommendation_throwsConflict()
    {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        Interview existing = new Interview();
        existing.setId(1L);
        existing.setStatus(InterviewStatus.COMPLETED); // completed but no hiring signal recorded

        Slot slot = freeSlot(new User());

        InterviewRequestDTO dto = new InterviewRequestDTO();
        dto.setSlotId(slotId);

        when(interviewRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(slotRepository.findByPublicId(slotId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> interviewService.updateInterview(publicId, dto))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("requires a recommendation");

        verify(interviewRepository, never()).save(any());
    }

    @Test
    void updateInterview_recommendationOnNonCompletedInterview_throwsConflict()
    {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        Interview existing = new Interview();
        existing.setId(1L);
        existing.setStatus(InterviewStatus.SCHEDULED);
        existing.setRecommendation(InterviewRecommendation.YES); // a recommendation on an interview that has not happened

        Slot slot = freeSlot(new User());

        InterviewRequestDTO dto = new InterviewRequestDTO();
        dto.setSlotId(slotId);

        when(interviewRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(slotRepository.findByPublicId(slotId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> interviewService.updateInterview(publicId, dto))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("can only be set on a completed interview");

        verify(interviewRepository, never()).save(any());
    }

    @Test
    void updateInterview_completedWithRecommendation_succeeds()
    {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        Interview existing = new Interview();
        existing.setId(1L);
        existing.setStatus(InterviewStatus.COMPLETED);
        existing.setRecommendation(InterviewRecommendation.STRONG_YES);

        Slot slot = freeSlot(new User());

        InterviewRequestDTO dto = new InterviewRequestDTO();
        dto.setSlotId(slotId);

        when(interviewRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(slotRepository.findByPublicId(slotId)).thenReturn(Optional.of(slot));
        when(interviewRepository.save(existing)).thenReturn(existing);
        when(interviewMapper.toResponse(existing)).thenReturn(new InterviewResponseDTO());

        InterviewResponseDTO result = interviewService.updateInterview(publicId, dto);

        assertThat(result).isNotNull();
        verify(interviewRepository).save(existing);
    }

    @Test
    void patchInterview_withoutOptionalIds_reusesExistingAssociations()
    {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();

        User interviewer = new User();
        Slot slot = freeSlot(interviewer);

        Interview existing = new Interview();
        existing.setId(1L);
        existing.setApplication(application(UUID.randomUUID(), ApplicationStatus.INTERVIEW_SCHEDULED));
        existing.setSlot(slot); // also wires slot.interview = existing

        InterviewRequestDTO dto = new InterviewRequestDTO(); // all ids null

        when(interviewRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(interviewRepository.save(existing)).thenReturn(existing);
        when(interviewMapper.toResponse(existing)).thenReturn(new InterviewResponseDTO());

        interviewService.patchInterview(publicId, dto);

        verify(applicationRepository, never()).findByPublicId(any());
        verify(slotRepository, never()).findByPublicId(any());
        verify(interviewMapper).patchEntity(dto, interviewer, slot, existing);
        verify(interviewRepository).save(existing);
    }

    @Test
    void deleteInterview_unknown_throwsResourceNotFound()
    {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();
        when(interviewRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.deleteInterview(publicId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRepository, never()).delete(any());
    }

    @Test
    void deleteInterview_found_freesSlotAndDeletes()
    {
        authenticateAsAdmin();
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

    private InterviewRequestDTO request(UUID applicationId, UUID slotId)
    {
        InterviewRequestDTO dto = new InterviewRequestDTO();
        dto.setApplicationId(applicationId);
        dto.setSlotId(slotId);
        return dto;
    }

    private Application application(UUID publicId, ApplicationStatus status)
    {
        Application application = new Application();
        application.setPublicId(publicId);
        application.setCandidate(user(UUID.randomUUID()));
        application.setJob(job(UUID.randomUUID()));
        application.setStatus(status);
        return application;
    }

    private User user(UUID publicId)
    {
        User user = new User();
        user.setPublicId(publicId);
        return user;
    }

    private Job job(UUID publicId)
    {
        return TestJobs.withPublicId(TestJobs.draft("Backend Engineer"), publicId);
    }

    private Slot freeSlot(User interviewer)
    {
        Slot slot = new Slot();
        slot.setInterviewer(interviewer);
        return slot;
    }

    private void authenticateAsAdmin()
    {
        User admin = User.builder()
                .id(1L)
                .publicId(UUID.randomUUID())
                .email("admin@test.local")
                .password("encoded")
                .roles(Set.of(UserRole.ADMIN))
                .isApproved(true)
                .build();
        CustomUserDetails principal = new CustomUserDetails(admin);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }
}
