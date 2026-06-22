package com.krino.backend.service;

import com.krino.backend.dto.application.ApplicationCreateDTO;
import com.krino.backend.dto.application.ApplicationResponseDTO;
import com.krino.backend.dto.application.ApplicationUpdateDTO;
import com.krino.backend.entity.Application;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.JobStatus;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.ApplicationMapper;
import com.krino.backend.repository.ApplicationRepository;
import com.krino.backend.repository.JobRepository;
import com.krino.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationServiceTest
{
    private static final long CANDIDATE_ID = 1L;
    private static final String CANDIDATE_EMAIL = "candidate@test.local";

    private ApplicationRepository applicationRepository;
    private JobRepository jobRepository;
    private UserRepository userRepository;
    private ApplicationMapper applicationMapper;
    private ApplicationService applicationService;

    @BeforeEach
    void setUp()
    {
        applicationRepository = mock(ApplicationRepository.class);
        jobRepository = mock(JobRepository.class);
        userRepository = mock(UserRepository.class);
        applicationMapper = mock(ApplicationMapper.class);
        applicationService = new ApplicationService(applicationRepository, jobRepository, userRepository,
                applicationMapper);
    }

    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createApplication_openJob_savesAndReturnsResponse()
    {
        UUID jobId = UUID.randomUUID();
        Job job = job(jobId, JobStatus.OPEN, null);
        User candidate = authenticateAsCandidate();

        ApplicationCreateDTO dto = new ApplicationCreateDTO(jobId, "https://cv.example/me.pdf");
        Application entity = new Application();
        Application saved = new Application();
        ApplicationResponseDTO response = new ApplicationResponseDTO();

        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByJobAndCandidate(job, candidate)).thenReturn(false);
        when(applicationMapper.toEntity(dto, job, candidate)).thenReturn(entity);
        when(applicationRepository.save(entity)).thenReturn(saved);
        when(applicationMapper.toResponse(saved)).thenReturn(response);

        ApplicationResponseDTO result = applicationService.createApplication(dto);

        assertThat(result).isSameAs(response);
        verify(applicationRepository).save(entity);
    }

    @Test
    void createApplication_unknownJob_throwsResourceNotFound()
    {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.createApplication(new ApplicationCreateDTO(jobId, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void createApplication_jobNotOpen_throwsConflict()
    {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.of(job(jobId, JobStatus.CLOSED, null)));

        assertThatThrownBy(() -> applicationService.createApplication(new ApplicationCreateDTO(jobId, null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("not open");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void createApplication_deadlinePassed_throwsConflict()
    {
        UUID jobId = UUID.randomUUID();
        Job job = job(jobId, JobStatus.OPEN, LocalDate.now().minusDays(1));
        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> applicationService.createApplication(new ApplicationCreateDTO(jobId, null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("deadline");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void createApplication_duplicateApplication_throwsConflict()
    {
        UUID jobId = UUID.randomUUID();
        Job job = job(jobId, JobStatus.OPEN, null);
        User candidate = authenticateAsCandidate();

        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByJobAndCandidate(job, candidate)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(new ApplicationCreateDTO(jobId, null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already applied");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void getApplicationByPublicId_unknown_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationByPublicId(publicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateApplication_unknown_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.updateApplication(publicId, new ApplicationUpdateDTO()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void updateApplication_withJobId_resolvesJobAndSaves()
    {
        UUID publicId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Application existing = new Application();
        Job job = job(jobId, JobStatus.OPEN, null);

        ApplicationUpdateDTO dto = new ApplicationUpdateDTO();
        dto.setJobId(jobId);

        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.of(job));
        when(applicationRepository.save(existing)).thenReturn(existing);
        when(applicationMapper.toResponse(existing)).thenReturn(new ApplicationResponseDTO());

        applicationService.updateApplication(publicId, dto);

        verify(applicationMapper).updateEntity(dto, job, existing);
        verify(applicationRepository).save(existing);
    }

    @Test
    void updateApplication_withUnknownJobId_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        ApplicationUpdateDTO dto = new ApplicationUpdateDTO();
        dto.setJobId(jobId);

        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.of(new Application()));
        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.updateApplication(publicId, dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void patchApplication_withoutJobId_passesNullJobToMapper()
    {
        UUID publicId = UUID.randomUUID();
        Application existing = new Application();
        ApplicationUpdateDTO dto = new ApplicationUpdateDTO();
        dto.setResumeUrl("https://cv.example/updated.pdf");

        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(existing)).thenReturn(existing);
        when(applicationMapper.toResponse(existing)).thenReturn(new ApplicationResponseDTO());

        applicationService.patchApplication(publicId, dto);

        verify(jobRepository, never()).findByPublicId(any());
        verify(applicationMapper).patchEntity(eq(dto), isNull(), eq(existing));
    }

    @Test
    void deleteApplication_unknown_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.deleteApplication(publicId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(applicationRepository, never()).delete(any());
    }

    @Test
    void deleteApplication_found_deletesApplication()
    {
        UUID publicId = UUID.randomUUID();
        Application application = new Application();
        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.of(application));

        applicationService.deleteApplication(publicId);

        verify(applicationRepository).delete(application);
    }

    private Job job(UUID publicId, JobStatus status, LocalDate deadline)
    {
        Job job = new Job();
        job.setPublicId(publicId);
        job.setTitle("Backend Engineer");
        job.setStatus(status);
        job.setApplyingDeadline(deadline);
        return job;
    }

    private User authenticateAsCandidate()
    {
        User candidate = User.builder()
                .id(CANDIDATE_ID)
                .publicId(UUID.randomUUID())
                .email(CANDIDATE_EMAIL)
                .roles(Set.of(UserRole.CANDIDATE))
                .build();

        CustomUserDetails principal = new CustomUserDetails(candidate);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                principal.getAuthorities()));
        SecurityContextHolder.setContext(context);

        when(userRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(candidate));
        return candidate;
    }
}
