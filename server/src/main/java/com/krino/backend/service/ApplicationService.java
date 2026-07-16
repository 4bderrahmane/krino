package com.krino.backend.service;

import com.krino.backend.dto.application.ApplicationCreateDTO;
import com.krino.backend.dto.application.ApplicationResponseDTO;
import com.krino.backend.dto.application.ApplicationUpdateDTO;
import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.entity.Application;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.JobStatus;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.ApplicationMapper;
import com.krino.backend.repository.ApplicationRepository;
import com.krino.backend.repository.JobRepository;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.service.resume.ResumeStorageService;
import com.krino.backend.service.resume.StoredResume;
import com.krino.backend.utility.ErrorCode;
import com.krino.backend.utility.SecurityUtilities;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ApplicationService {

    private static final String ADMIN = "ADMIN";
    private static final String HR_MANAGER = "HR_MANAGER";
    private static final String JOB_TITLE = "jobTitle";
    private static final String PUBLIC_ID = "publicId";
    private static final String DEFAULT_RESUME_FILENAME = "resume.pdf";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;
    private final ResumeStorageService resumeStorageService;

    public ApplicationResponseDTO createApplication(ApplicationCreateDTO applicationCreateDTO) {
        Job job = resolveJob(applicationCreateDTO.getJobId());

        if (job.getStatus() != JobStatus.OPEN) {
            throw new ResourceConflictException(
                    String.format("Job '%s' is not open for applications.", job.getTitle()),
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(JOB_TITLE, job.getTitle()));
        }
        if (job.getApplicationDeadline() != null && job.getApplicationDeadline().isBefore(Instant.now())) {
            throw new ResourceConflictException(
                    String.format("The application deadline for job '%s' has passed.", job.getTitle()),
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(JOB_TITLE, job.getTitle(), "applicationDeadline", job.getApplicationDeadline().toString()));
        }

        User candidate = getCurrentUser();

        if (applicationRepository.existsByJobAndCandidate(job, candidate)) {
            throw new ResourceConflictException(
                    String.format("You have already applied to job '%s'.", job.getTitle()),
                    ErrorCode.DATA_CONFLICT,
                    Map.of(JOB_TITLE, job.getTitle()));
        }

        Application application = applicationMapper.toEntity(applicationCreateDTO, job, candidate);

        Application savedApplication = applicationRepository.save(application);
        return applicationMapper.toResponse(savedApplication);
    }

    private Job resolveJob(UUID jobPublicId) {
        return jobRepository.findByPublicId(jobPublicId)
                .orElseThrow(() -> new ResourceNotFoundException(Job.class.getSimpleName(), PUBLIC_ID, jobPublicId));
    }

    private User getCurrentUser() {
        CustomUserDetails currentUser = SecurityUtilities.requireCurrentCustomUser();
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("No authenticated user to apply as"));
    }

    public ApplicationResponseDTO getApplicationByPublicId(UUID publicId) {
        Application application = applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getSimpleName(), PUBLIC_ID,
                        publicId));
        requireApplicationOwnerOrStaff(application);
        return applicationMapper.toResponse(application);
    }

    public PageResponse<ApplicationResponseDTO> getAllApplications(Pageable pageable) {
        SecurityUtilities.requireAnyRole(ADMIN, HR_MANAGER);
        return PageResponse.from(applicationRepository.findAll(pageable),
                applicationMapper::toResponse);
    }

    public PageResponse<ApplicationResponseDTO> getMyApplications(Pageable pageable) {
        User candidate = getCurrentUser();
        return PageResponse.from(applicationRepository.findByCandidate(candidate, pageable),
                applicationMapper::toResponse);
    }

    public ApplicationResponseDTO updateApplication(UUID publicId, ApplicationUpdateDTO applicationUpdateDTO) {
        Application existingApplication = applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getSimpleName(), PUBLIC_ID,
                        publicId));
        requireApplicationOwnerOrStaff(existingApplication);
        enforceStatusUpdatePolicy(applicationUpdateDTO);
        enforceJobImmutable(existingApplication, applicationUpdateDTO);

        applicationMapper.updateEntity(applicationUpdateDTO, existingApplication);
        Application updatedApplication = applicationRepository.save(existingApplication);
        return applicationMapper.toResponse(updatedApplication);
    }

    public ApplicationResponseDTO patchApplication(UUID publicId, ApplicationUpdateDTO applicationUpdateDTO) {
        Application existingApplication = applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getSimpleName(), PUBLIC_ID,
                        publicId));
        requireApplicationOwnerOrStaff(existingApplication);
        enforceStatusUpdatePolicy(applicationUpdateDTO);
        enforceJobImmutable(existingApplication, applicationUpdateDTO);

        applicationMapper.patchEntity(applicationUpdateDTO, existingApplication);

        Application patchedApplication = applicationRepository.save(existingApplication);
        return applicationMapper.toResponse(patchedApplication);
    }

    public void deleteApplication(UUID publicId) {
        Application application = applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getSimpleName(), PUBLIC_ID,
                        publicId));
        requireApplicationOwnerOrStaff(application);
        if (StringUtils.hasText(application.getResumeObjectKey())) {
            resumeStorageService.deleteResume(application.getResumeObjectKey());
        }
        applicationRepository.delete(application);
    }

    public ApplicationResponseDTO uploadResume(UUID publicId, MultipartFile resume) {
        Application application = findApplication(publicId);
        requireApplicationOwnerOrStaff(application);

        String previousObjectKey = application.getResumeObjectKey();
        StoredResume storedResume = resumeStorageService.uploadResume(application.getPublicId(), resume);
        applyResumeMetadata(application, storedResume);

        Application savedApplication = applicationRepository.save(application);
        if (StringUtils.hasText(previousObjectKey) && !previousObjectKey.equals(storedResume.objectKey())) {
            resumeStorageService.deleteResumeBestEffort(previousObjectKey);
        }
        return applicationMapper.toResponse(savedApplication);
    }

    /**
     * Attach the candidate's base CV (captured at registration) to this application by
     * copying it into the application's own storage, so editing one never affects the other.
     */
    public ApplicationResponseDTO applyBaseResume(UUID publicId) {
        Application application = findApplication(publicId);
        requireApplicationOwnerOrStaff(application);

        User candidate = application.getCandidate();
        if (candidate == null || !StringUtils.hasText(candidate.getResumeObjectKey())) {
            throw new ResourceConflictException(
                    "You don't have a base CV to reuse. Please upload a CV for this application.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(PUBLIC_ID, publicId.toString()));
        }

        String previousObjectKey = application.getResumeObjectKey();
        String copiedObjectKey = resumeStorageService.copyResumeForApplication(candidate.getResumeObjectKey(),
                application.getPublicId());

        StoredResume storedResume = new StoredResume(
                copiedObjectKey,
                StringUtils.hasText(candidate.getResumeOriginalFilename())
                        ? candidate.getResumeOriginalFilename()
                        : DEFAULT_RESUME_FILENAME,
                StringUtils.hasText(candidate.getResumeContentType())
                        ? candidate.getResumeContentType()
                        : PDF_CONTENT_TYPE,
                candidate.getResumeSizeBytes(),
                Instant.now());
        applyResumeMetadata(application, storedResume);

        Application savedApplication = applicationRepository.save(application);
        if (StringUtils.hasText(previousObjectKey) && !previousObjectKey.equals(copiedObjectKey)) {
            resumeStorageService.deleteResumeBestEffort(previousObjectKey);
        }
        return applicationMapper.toResponse(savedApplication);
    }

    public ResumeDownload downloadResume(UUID publicId) {
        Application application = findApplication(publicId);
        requireApplicationOwnerOrStaff(application);
        if (!StringUtils.hasText(application.getResumeObjectKey())) {
            throw new ResourceNotFoundException("Resume file not found for this application.");
        }

        InputStream inputStream = resumeStorageService.downloadResume(application.getResumeObjectKey());
        return new ResumeDownload(
                StringUtils.hasText(application.getResumeOriginalFilename())
                        ? application.getResumeOriginalFilename()
                        : DEFAULT_RESUME_FILENAME,
                StringUtils.hasText(application.getResumeContentType())
                        ? application.getResumeContentType()
                        : PDF_CONTENT_TYPE,
                application.getResumeSizeBytes(),
                inputStream);
    }

    public void deleteResume(UUID publicId) {
        Application application = findApplication(publicId);
        requireApplicationOwnerOrStaff(application);
        if (!StringUtils.hasText(application.getResumeObjectKey())) {
            return;
        }

        resumeStorageService.deleteResume(application.getResumeObjectKey());
        clearResumeMetadata(application);
        applicationRepository.save(application);
    }

    private Application findApplication(UUID publicId) {
        return applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getSimpleName(), PUBLIC_ID,
                        publicId));
    }

    private void applyResumeMetadata(Application application, StoredResume storedResume) {
        application.setResumeObjectKey(storedResume.objectKey());
        application.setResumeOriginalFilename(storedResume.originalFilename());
        application.setResumeContentType(storedResume.contentType());
        application.setResumeSizeBytes(storedResume.sizeBytes());
        application.setResumeUploadedAt(storedResume.uploadedAt());
    }

    private void clearResumeMetadata(Application application) {
        application.setResumeObjectKey(null);
        application.setResumeOriginalFilename(null);
        application.setResumeContentType(null);
        application.setResumeSizeBytes(null);
        application.setResumeUploadedAt(null);
    }

    /**
     * The application {@code status} is the employer's decision (under review, accepted,
     * rejected, ...). Only ADMIN/HR may set it; a candidate editing their own application
     * cannot change it, so we drop the field and let the mapper preserve the existing value.
     */
    private void enforceStatusUpdatePolicy(ApplicationUpdateDTO applicationUpdateDTO) {
        if (!SecurityUtilities.hasAnyRole(ADMIN, HR_MANAGER)) {
            applicationUpdateDTO.setStatus(null);
        }
    }

    /**
     * An application is permanently tied to the job it was created for. Letting the job
     * change would move an existing application onto another posting, skipping the
     * open-status, deadline and duplicate-application checks enforced at creation, so any
     * attempt to point it at a different job is rejected.
     */
    private void enforceJobImmutable(Application application, ApplicationUpdateDTO applicationUpdateDTO) {
        UUID requestedJobId = applicationUpdateDTO.getJobId();
        if (requestedJobId == null) {
            return;
        }
        Job currentJob = application.getJob();
        if (currentJob == null || !requestedJobId.equals(currentJob.getPublicId())) {
            throw new ResourceConflictException(
                    "An application's job cannot be changed.",
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of("resource", "Application"));
        }
    }

    private void requireApplicationOwnerOrStaff(Application application) {
        if (SecurityUtilities.hasAnyRole(ADMIN, HR_MANAGER)) {
            return;
        }
        User candidate = application.getCandidate();
        if (candidate == null || candidate.getPublicId() == null) {
            throw new AccessDeniedException("You do not have permission to access this application.");
        }
        SecurityUtilities.requireCurrentUser(candidate.getPublicId());
    }

    public record ResumeDownload(
            String originalFilename,
            String contentType,
            Long sizeBytes,
            InputStream inputStream
    ) {
    }
}
