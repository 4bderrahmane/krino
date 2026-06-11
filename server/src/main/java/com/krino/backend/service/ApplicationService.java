package com.krino.backend.service;

import com.krino.backend.dto.application.ApplicationCreateDTO;
import com.krino.backend.dto.application.ApplicationResponseDTO;
import com.krino.backend.dto.application.ApplicationUpdateDTO;
import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.entity.Application;
import com.krino.backend.entity.Job;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.repository.ApplicationRepository;
import com.krino.backend.repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ApplicationService
{

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ModelMapper modelMapper;

    public ApplicationResponseDTO createApplication(ApplicationCreateDTO applicationCreateDTO)
    {
        Application application = modelMapper.map(applicationCreateDTO, Application.class);
        application.setJob(resolveJob(applicationCreateDTO.getJobId()));
        Application savedApplication = applicationRepository.save(application);
        return modelMapper.map(savedApplication, ApplicationResponseDTO.class);
    }

    private Job resolveJob(UUID jobPublicId)
    {
        return jobRepository.findByPublicId(jobPublicId)
                .orElseThrow(() -> new ResourceNotFoundException(Job.class.getSimpleName(), "publicId", jobPublicId));
    }

    public ApplicationResponseDTO getApplicationByPublicId(UUID publicId)
    {
        Application application = applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getSimpleName(), "publicId", publicId));
        return modelMapper.map(application, ApplicationResponseDTO.class);
    }

    public PageResponse<ApplicationResponseDTO> getAllApplications(Pageable pageable)
    {
        return PageResponse.from(applicationRepository.findAll(pageable),
                application -> modelMapper.map(application, ApplicationResponseDTO.class));
    }

    public ApplicationResponseDTO updateApplication(UUID publicId, ApplicationUpdateDTO applicationUpdateDTO)
    {
        Application existingApplication = applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getSimpleName(), "publicId", publicId));

        modelMapper.map(applicationUpdateDTO, existingApplication);
        if (applicationUpdateDTO.getJobId() != null)
        {
            existingApplication.setJob(resolveJob(applicationUpdateDTO.getJobId()));
        }
        Application updatedApplication = applicationRepository.save(existingApplication);
        return modelMapper.map(updatedApplication, ApplicationResponseDTO.class);
    }

    public ApplicationResponseDTO patchApplication(UUID publicId, ApplicationUpdateDTO applicationUpdateDTO)
    {
        Application existingApplication = applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getSimpleName(), "publicId", publicId));

        if (applicationUpdateDTO.getStatus() != null)
        {
            existingApplication.setStatus(applicationUpdateDTO.getStatus());
        }
        if (applicationUpdateDTO.getResumeUrl() != null)
        {
            existingApplication.setResumeUrl(applicationUpdateDTO.getResumeUrl());
        }
        if (applicationUpdateDTO.getJobId() != null)
        {
            existingApplication.setJob(resolveJob(applicationUpdateDTO.getJobId()));
        }

        Application patchedApplication = applicationRepository.save(existingApplication);
        return modelMapper.map(patchedApplication, ApplicationResponseDTO.class);
    }

    public void deleteApplication(UUID publicId)
    {
        Application application = applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getSimpleName(), "publicId", publicId));
        applicationRepository.delete(application);
    }
}
