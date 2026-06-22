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
import com.krino.backend.utility.ErrorCode;
import com.krino.backend.utility.SecurityUtilities;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ApplicationService
{

    private static final String JOB_TITLE = "jobTitle";
    private static final String PUBLIC_ID = "publicId";
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;

    public ApplicationResponseDTO createApplication(ApplicationCreateDTO applicationCreateDTO)
    {
        Job job = resolveJob(applicationCreateDTO.getJobId());

        if (job.getStatus() != JobStatus.OPEN)
        {
            throw new ResourceConflictException(
                    String.format("Job '%s' is not open for applications.", job.getTitle()),
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(JOB_TITLE, job.getTitle()));
        }
        if (job.getApplyingDeadline() != null && job.getApplyingDeadline().isBefore(LocalDate.now()))
        {
            throw new ResourceConflictException(
                    String.format("The applying deadline for job '%s' has passed.", job.getTitle()),
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of(JOB_TITLE, job.getTitle(), "applyingDeadline", job.getApplyingDeadline().toString()));
        }

        User candidate = getCurrentUser();

        if (applicationRepository.existsByJobAndCandidate(job, candidate))
        {
            throw new ResourceConflictException(
                    String.format("You have already applied to job '%s'.", job.getTitle()),
                    ErrorCode.DATA_CONFLICT,
                    Map.of(JOB_TITLE, job.getTitle()));
        }

        Application application = applicationMapper.toEntity(applicationCreateDTO, job, candidate);

        Application savedApplication = applicationRepository.save(application);
        return applicationMapper.toResponse(savedApplication);
    }

    private Job resolveJob(UUID jobPublicId)
    {
        return jobRepository.findByPublicId(jobPublicId)
                .orElseThrow(() -> new ResourceNotFoundException(Job.class.getSimpleName(), PUBLIC_ID, jobPublicId));
    }

    private User getCurrentUser()
    {
        return SecurityUtilities.getCurrentCustomUser()
                .map(CustomUserDetails::getId)
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new AccessDeniedException("No authenticated user to apply as"));
    }

    public ApplicationResponseDTO getApplicationByPublicId(UUID publicId)
    {
        Application application = applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getSimpleName(), PUBLIC_ID, publicId));
        return applicationMapper.toResponse(application);
    }

    public PageResponse<ApplicationResponseDTO> getAllApplications(Pageable pageable)
    {
        return PageResponse.from(applicationRepository.findAll(pageable),
                applicationMapper::toResponse);
    }

    public ApplicationResponseDTO updateApplication(UUID publicId, ApplicationUpdateDTO applicationUpdateDTO)
    {
        Application existingApplication = applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getSimpleName(), PUBLIC_ID, publicId));

        Job job = applicationUpdateDTO.getJobId() != null
                ? resolveJob(applicationUpdateDTO.getJobId())
                : null;
        applicationMapper.updateEntity(applicationUpdateDTO, job, existingApplication);
        Application updatedApplication = applicationRepository.save(existingApplication);
        return applicationMapper.toResponse(updatedApplication);
    }

    public ApplicationResponseDTO patchApplication(UUID publicId, ApplicationUpdateDTO applicationUpdateDTO)
    {
        Application existingApplication = applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getSimpleName(), PUBLIC_ID, publicId));

        Job job = applicationUpdateDTO.getJobId() != null
                ? resolveJob(applicationUpdateDTO.getJobId())
                : null;
        applicationMapper.patchEntity(applicationUpdateDTO, job, existingApplication);

        Application patchedApplication = applicationRepository.save(existingApplication);
        return applicationMapper.toResponse(patchedApplication);
    }

    public void deleteApplication(UUID publicId)
    {
        Application application = applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getSimpleName(), PUBLIC_ID, publicId));
        applicationRepository.delete(application);
    }
}
