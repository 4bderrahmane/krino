package com.jesa.interviewslotmanager.service;

import com.jesa.interviewslotmanager.dto.application.ApplicationCreateDTO;
import com.jesa.interviewslotmanager.dto.application.ApplicationResponseDTO;
import com.jesa.interviewslotmanager.dto.application.ApplicationUpdateDTO;
import com.jesa.interviewslotmanager.entity.Application;
import com.jesa.interviewslotmanager.exception.ResourceNotFoundException;
import com.jesa.interviewslotmanager.repository.ApplicationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ApplicationService
{

    private final ApplicationRepository applicationRepository;
    private final ModelMapper modelMapper;

    public ApplicationResponseDTO createApplication(ApplicationCreateDTO applicationCreateDTO)
    {
        Application application = modelMapper.map(applicationCreateDTO, Application.class);
        Application savedApplication = applicationRepository.save(application);
        return modelMapper.map(savedApplication, ApplicationResponseDTO.class);
    }

    public ApplicationResponseDTO getApplicationById(Long applicationId)
    {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getName(), "id", applicationId));
        return modelMapper.map(application, ApplicationResponseDTO.class);
    }

    public List<ApplicationResponseDTO> getAllApplications()
    {
        return applicationRepository.findAll().stream()
                .map(application -> modelMapper.map(application, ApplicationResponseDTO.class))
                .toList();
    }

    public ApplicationResponseDTO updateApplication(Long applicationId, ApplicationUpdateDTO applicationUpdateDTO)
    {
        Application existingApplication = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getName(), "id", applicationId));

        modelMapper.map(applicationUpdateDTO, existingApplication);
        Application updatedApplication = applicationRepository.save(existingApplication);
        return modelMapper.map(updatedApplication, ApplicationResponseDTO.class);
    }

    public ApplicationResponseDTO patchApplication(Long applicationId, ApplicationUpdateDTO applicationUpdateDTO)
    {
        Application existingApplication = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(Application.class.getName(), "id", applicationId));

        if (applicationUpdateDTO.getStatus() != null)
        {
            existingApplication.setStatus(applicationUpdateDTO.getStatus());
        }
        if (applicationUpdateDTO.getResumeUrl() != null)
        {
            existingApplication.setResumeUrl(applicationUpdateDTO.getResumeUrl());
        }

        Application patchedApplication = applicationRepository.save(existingApplication);
        return modelMapper.map(patchedApplication, ApplicationResponseDTO.class);
    }

    public void deleteApplication(Long applicationId)
    {
        if (!applicationRepository.existsById(applicationId))
        {
            throw new ResourceNotFoundException(Application.class.getName(), "id", applicationId);
        }
        applicationRepository.deleteById(applicationId);
    }
}