package com.jesa.interviewslotmanager.service;

import com.jesa.interviewslotmanager.dto.application.ApplicationCreateDTO;
import com.jesa.interviewslotmanager.entity.Application;
import com.jesa.interviewslotmanager.entity.Job;
import com.jesa.interviewslotmanager.entity.User;
import com.jesa.interviewslotmanager.exception.JobNotFoundException;
import com.jesa.interviewslotmanager.exception.UserNotFoundException;
import com.jesa.interviewslotmanager.repository.ApplicationRepository;
import com.jesa.interviewslotmanager.repository.JobRepository;
import com.jesa.interviewslotmanager.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationService
{
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public Application createApplication(ApplicationCreateDTO request)
    {

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new JobNotFoundException("ID", String.valueOf(request.getJobId())));

//        Job job = jobRepository.findByTitle(request.getJobTitle())
//                .orElseThrow(() -> new JobNotFoundException("Title", jobRepository.findById(request.getJobId()).get().getTitle()));
        //.orElseThrow(() -> new JobNotFoundException("Title", jobRepository.findById(request.getJobId()).get().getTitle() ));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User candidate = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("email", email));

        Application application = new Application();
        application.setJob(job);
        application.setCandidate(candidate);
        application.setResumeUrl(request.getResumeUrl());
        application.setAppliedAt(request.getAppliedAt());
        return applicationRepository.save(application);
    }
}
