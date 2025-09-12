package com.jesa.interviewslotmanager.controller;

import com.jesa.interviewslotmanager.dto.job.JobCreateDTO;
import com.jesa.interviewslotmanager.dto.job.JobResponseDTO;
import com.jesa.interviewslotmanager.entity.Job;
import com.jesa.interviewslotmanager.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController
{

    private final JobService jobService;
    private final ModelMapper modelMapper;

    @PostMapping("/create")
    public ResponseEntity<JobResponseDTO> createJob(@Valid @RequestBody JobCreateDTO jobRequest)
    {
        Job job = jobService.createJob(jobRequest);
        JobResponseDTO response = modelMapper.map(job, JobResponseDTO.class);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<String> deleteJobById(Long id)
    {
        jobService.deleteJobById(id);
        return ResponseEntity.ok("Job deleted");
    }
}
