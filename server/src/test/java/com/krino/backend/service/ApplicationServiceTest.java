package com.krino.backend.service;

import com.krino.backend.dto.application.ApplicationCreateDTO;
import com.krino.backend.dto.application.ApplicationResponseDTO;
import com.krino.backend.dto.application.ApplicationUpdateDTO;
import com.krino.backend.entity.Application;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.ApplicationStatus;
import com.krino.backend.entity.enums.JobStatus;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.support.TestJobs;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
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

class ApplicationServiceTest {
    private static final long CANDIDATE_ID = 1L;
    private static final String CANDIDATE_EMAIL = "candidate@test.local";

    private ApplicationRepository applicationRepository;
    private JobRepository jobRepository;
    private UserRepository userRepository;
    private ApplicationMapper applicationMapper;
    private CvStorageService cvStorageService;
    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationRepository = mock(ApplicationRepository.class);
        jobRepository = mock(JobRepository.class);
        userRepository = mock(UserRepository.class);
        applicationMapper = mock(ApplicationMapper.class);
        cvStorageService = mock(CvStorageService.class);
        applicationService = new ApplicationService(applicationRepository, jobRepository, userRepository, applicationMapper, cvStorageService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createApplication_openJob_savesAndReturnsResponse() {
        UUID jobId = UUID.randomUUID();
        Job job = job(jobId, JobStatus.OPEN, null);
        User candidate = authenticateAsCandidate();

        ApplicationCreateDTO dto = new ApplicationCreateDTO(jobId);
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
    void createApplication_unknownJob_throwsResourceNotFound() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.empty());

        ApplicationCreateDTO request = new ApplicationCreateDTO(jobId);
        assertThatThrownBy(() -> applicationService.createApplication(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void createApplication_jobNotOpen_throwsConflict() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.of(job(jobId, JobStatus.CLOSED, null)));

        ApplicationCreateDTO request = new ApplicationCreateDTO(jobId);
        assertThatThrownBy(() -> applicationService.createApplication(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("not open");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void createApplication_deadlinePassed_throwsConflict() {
        UUID jobId = UUID.randomUUID();
        Job job = job(jobId, JobStatus.OPEN, Instant.parse("2020-01-01T00:00:00Z"));
        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.of(job));

        ApplicationCreateDTO request = new ApplicationCreateDTO(jobId);
        assertThatThrownBy(() -> applicationService.createApplication(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("deadline");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void createApplication_duplicateApplication_throwsConflict() {
        UUID jobId = UUID.randomUUID();
        Job job = job(jobId, JobStatus.OPEN, null);
        User candidate = authenticateAsCandidate();

        when(jobRepository.findByPublicId(jobId)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByJobAndCandidate(job, candidate)).thenReturn(true);

        ApplicationCreateDTO request = new ApplicationCreateDTO(jobId);
        assertThatThrownBy(() -> applicationService.createApplication(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already applied");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void getApplicationByPublicId_unknown_throwsResourceNotFound() {
        UUID publicId = UUID.randomUUID();
        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationByPublicId(publicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateApplication_unknown_throwsResourceNotFound() {
        UUID publicId = UUID.randomUUID();
        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        ApplicationUpdateDTO update = new ApplicationUpdateDTO();
        assertThatThrownBy(() -> applicationService.updateApplication(publicId, update))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void updateApplication_sameJob_succeeds() {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Application existing = new Application();
        existing.setJob(job(jobId, JobStatus.OPEN, null));

        ApplicationUpdateDTO dto = new ApplicationUpdateDTO();
        dto.setJobId(jobId);

        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(existing)).thenReturn(existing);
        when(applicationMapper.toResponse(existing)).thenReturn(new ApplicationResponseDTO());

        applicationService.updateApplication(publicId, dto);

        verify(applicationMapper).updateEntity(dto, existing);
        verify(applicationRepository).save(existing);
    }

    @Test
    void updateApplication_changingJob_throwsConflict() {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();
        Application existing = new Application();
        existing.setJob(job(UUID.randomUUID(), JobStatus.OPEN, null));

        ApplicationUpdateDTO dto = new ApplicationUpdateDTO();
        dto.setJobId(UUID.randomUUID());

        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> applicationService.updateApplication(publicId, dto))
                .isInstanceOf(ResourceConflictException.class);

        verify(applicationRepository, never()).save(any());
        verify(applicationMapper, never()).updateEntity(any(), any());
    }

    @Test
    void patchApplication_withoutJobId_passesDtoToMapper() {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();
        Application existing = new Application();
        ApplicationUpdateDTO dto = new ApplicationUpdateDTO();

        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(existing)).thenReturn(existing);
        when(applicationMapper.toResponse(existing)).thenReturn(new ApplicationResponseDTO());

        applicationService.patchApplication(publicId, dto);

        verify(jobRepository, never()).findByPublicId(any());
        verify(applicationMapper).patchEntity(dto, existing);
    }

    @Test
    void patchApplication_candidate_cannotChangeStatus() {
        User candidate = authenticateAsCandidate();
        UUID publicId = UUID.randomUUID();
        Application existing = new Application();
        existing.setCandidate(candidate);

        ApplicationUpdateDTO dto = new ApplicationUpdateDTO();
        dto.setStatus(ApplicationStatus.ACCEPTED);

        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(existing)).thenReturn(existing);
        when(applicationMapper.toResponse(existing)).thenReturn(new ApplicationResponseDTO());

        applicationService.patchApplication(publicId, dto);

        assertThat(dto.getStatus()).isNull();
        verify(applicationMapper).patchEntity(dto, existing);
    }

    @Test
    void patchApplication_staff_keepsStatus() {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();
        Application existing = new Application();

        ApplicationUpdateDTO dto = new ApplicationUpdateDTO();
        dto.setStatus(ApplicationStatus.ACCEPTED);

        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(existing)).thenReturn(existing);
        when(applicationMapper.toResponse(existing)).thenReturn(new ApplicationResponseDTO());

        applicationService.patchApplication(publicId, dto);

        assertThat(dto.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        verify(applicationMapper).patchEntity(dto, existing);
    }

    @Test
    void deleteApplication_unknown_throwsResourceNotFound() {
        UUID publicId = UUID.randomUUID();
        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.deleteApplication(publicId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(applicationRepository, never()).delete(any());
    }

    @Test
    void deleteApplication_found_deletesApplication() {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();
        Application application = new Application();
        application.setResumeObjectKey("applications/%s/resume/old.pdf".formatted(publicId));
        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.of(application));

        applicationService.deleteApplication(publicId);

        verify(cvStorageService).deleteResume(application.getResumeObjectKey());
        verify(applicationRepository).delete(application);
    }

    @Test
    void uploadResume_found_updatesMetadataAndDeletesPreviousResume() {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();
        LocalDateTime uploadedAt = LocalDateTime.of(2026, Month.JANUARY, 15, 10, 30);
        MultipartFile resume = mock(MultipartFile.class);
        Application application = new Application();
        application.setPublicId(publicId);
        application.setResumeObjectKey("applications/%s/resume/old.pdf".formatted(publicId));
        CvStorageService.StoredResume storedResume = new CvStorageService.StoredResume(
                "applications/%s/resume/new.pdf".formatted(publicId),
                "candidate.pdf",
                "application/pdf",
                1024L,
                uploadedAt);
        ApplicationResponseDTO response = new ApplicationResponseDTO();

        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.of(application));
        when(cvStorageService.uploadResume(publicId, resume)).thenReturn(storedResume);
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toResponse(application)).thenReturn(response);

        ApplicationResponseDTO result = applicationService.uploadResume(publicId, resume);

        assertThat(result).isSameAs(response);
        assertThat(application.getResumeObjectKey()).isEqualTo(storedResume.objectKey());
        assertThat(application.getResumeOriginalFilename()).isEqualTo("candidate.pdf");
        assertThat(application.getResumeContentType()).isEqualTo("application/pdf");
        assertThat(application.getResumeSizeBytes()).isEqualTo(1024L);
        assertThat(application.getResumeUploadedAt()).isEqualTo(uploadedAt);
        verify(cvStorageService).deleteResumeBestEffort("applications/%s/resume/old.pdf".formatted(publicId));
    }

    @Test
    void applyBaseResume_copiesCandidateBaseCvIntoApplication() {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();
        User candidate = User.builder()
                .publicId(UUID.randomUUID())
                .resumeObjectKey("users/base/resume/base.pdf")
                .resumeOriginalFilename("base-cv.pdf")
                .resumeContentType("application/pdf")
                .resumeSizeBytes(2048L)
                .build();
        Application application = new Application();
        application.setPublicId(publicId);
        application.setCandidate(candidate);
        ApplicationResponseDTO response = new ApplicationResponseDTO();

        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.of(application));
        when(cvStorageService.copyResumeForApplication("users/base/resume/base.pdf", publicId))
                .thenReturn("applications/%s/resume/copied.pdf".formatted(publicId));
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toResponse(application)).thenReturn(response);

        ApplicationResponseDTO result = applicationService.applyBaseResume(publicId);

        assertThat(result).isSameAs(response);
        assertThat(application.getResumeObjectKey()).isEqualTo("applications/%s/resume/copied.pdf".formatted(publicId));
        assertThat(application.getResumeOriginalFilename()).isEqualTo("base-cv.pdf");
        assertThat(application.getResumeSizeBytes()).isEqualTo(2048L);
    }

    @Test
    void applyBaseResume_withoutBaseCv_throwsConflict() {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();
        Application application = new Application();
        application.setPublicId(publicId);
        application.setCandidate(User.builder().publicId(UUID.randomUUID()).build());

        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.applyBaseResume(publicId))
                .isInstanceOf(ResourceConflictException.class);
        verify(cvStorageService, never()).copyResumeForApplication(any(), any());
    }

    @Test
    void downloadResume_withoutStoredResume_throwsResourceNotFound() {
        authenticateAsAdmin();
        UUID publicId = UUID.randomUUID();
        Application application = new Application();

        when(applicationRepository.findByPublicId(publicId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.downloadResume(publicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Job job(UUID publicId, JobStatus status, Instant deadline) {
        return TestJobs.withState(TestJobs.draft("Backend Engineer"), publicId, status, deadline);
    }

    private User authenticateAsCandidate() {
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

    private void authenticateAsAdmin() {
        User admin = User.builder()
                .id(99L)
                .publicId(UUID.randomUUID())
                .email("admin@test.local")
                .roles(Set.of(UserRole.ADMIN))
                .build();

        CustomUserDetails principal = new CustomUserDetails(admin);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }
}
