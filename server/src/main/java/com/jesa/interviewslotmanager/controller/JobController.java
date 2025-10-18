package com.jesa.interviewslotmanager.controller;

import com.jesa.interviewslotmanager.dto.job.JobCreateDTO;
import com.jesa.interviewslotmanager.dto.job.JobResponseDTO;
import com.jesa.interviewslotmanager.dto.job.JobUpdateDTO;
import com.jesa.interviewslotmanager.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController
{

    private final JobService jobService;

    @PostMapping("/create")
    public ResponseEntity<JobResponseDTO> createJob(@Valid @RequestBody JobCreateDTO jobRequest)
    {
        JobResponseDTO response = jobService.createJob(jobRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDTO> getJobById(@PathVariable("id") Long id) {
        JobResponseDTO job = jobService.getJobById(id);
        return ResponseEntity.ok(job);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponseDTO> updateJob(@PathVariable("id") Long id, @Valid @RequestBody JobUpdateDTO jobUpdateDTO) {
        JobResponseDTO updatedJob = jobService.updateJob(id, jobUpdateDTO);
        return ResponseEntity.ok(updatedJob);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JobResponseDTO> patchJob(@PathVariable("id") Long id, @Valid @RequestBody JobUpdateDTO jobUpdateDTO) {
        JobResponseDTO patchedJob = jobService.patchJob(id, jobUpdateDTO);
        return ResponseEntity.ok(patchedJob);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteJobById(@PathVariable("id") Long id)
    {
        jobService.deleteJobById(id);
        return ResponseEntity.ok("Job deleted");
    }
}
