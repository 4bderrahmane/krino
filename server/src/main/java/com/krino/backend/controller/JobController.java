package com.krino.backend.controller;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.job.JobCloseRequestDTO;
import com.krino.backend.dto.job.JobCreateDTO;
import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.dto.job.JobUpdateDTO;
import com.krino.backend.service.JobService;
import com.krino.backend.utility.SortWhitelist;
import com.krino.backend.validation.ValidationGroups;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Jobs")
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    private static final SortWhitelist SORT_WHITELIST = SortWhitelist.of(
            "id", "title", "status", "publishedAt", "applicationDeadline", "createdDate", "lastModifiedDate");

    @PostMapping
    @PreAuthorize("hasAuthority('job:create')")
    public ResponseEntity<JobResponseDTO> createJob(@Valid @RequestBody JobCreateDTO jobRequest) {
        JobResponseDTO response = jobService.createJob(jobRequest);
        return ResponseEntity.created(URI.create("/api/jobs/" + response.getId())).body(response);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('job:read')")
    public ResponseEntity<JobResponseDTO> getJobByPublicId(@PathVariable UUID publicId) {
        JobResponseDTO job = jobService.getJobByPublicId(publicId);
        return ResponseEntity.ok(job);
    }

    // The internal catalogue: every status, drafts included, so it is staff-only.
    // Candidates browse GET /api/public/jobs instead.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'INTERVIEWER')")
    public ResponseEntity<PageResponse<JobResponseDTO>> getAllJobs(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        PageResponse<JobResponseDTO> jobs = jobService.getAllJobs(SORT_WHITELIST.sanitize(pageable));
        return ResponseEntity.ok(jobs);
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('job:update')")
    public ResponseEntity<JobResponseDTO> updateJob(@PathVariable UUID publicId,
                                                    @Validated(ValidationGroups.FullUpdate.class)
                                                    @RequestBody JobUpdateDTO jobUpdateDTO) {
        JobResponseDTO updatedJob = jobService.updateJob(publicId, jobUpdateDTO);
        return ResponseEntity.ok(updatedJob);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('job:update')")
    public ResponseEntity<JobResponseDTO> patchJob(@PathVariable UUID publicId, @Valid @RequestBody JobUpdateDTO jobUpdateDTO) {
        JobResponseDTO patchedJob = jobService.patchJob(publicId, jobUpdateDTO);
        return ResponseEntity.ok(patchedJob);
    }

    @PostMapping("/{publicId}/publish")
    @PreAuthorize("hasAuthority('job:update')")
    public ResponseEntity<JobResponseDTO> publishJob(@PathVariable UUID publicId) {
        return ResponseEntity.ok(jobService.publishJob(publicId));
    }

    @PostMapping("/{publicId}/pause")
    @PreAuthorize("hasAuthority('job:update')")
    public ResponseEntity<JobResponseDTO> pauseJob(@PathVariable UUID publicId) {
        return ResponseEntity.ok(jobService.pauseJob(publicId));
    }

    @PostMapping("/{publicId}/close")
    @PreAuthorize("hasAuthority('job:update')")
    public ResponseEntity<JobResponseDTO> closeJob(@PathVariable UUID publicId, @Valid @RequestBody JobCloseRequestDTO closeRequest) {
        return ResponseEntity.ok(jobService.closeJob(publicId, closeRequest.getStatus()));
    }

    @PostMapping("/{publicId}/archive")
    @PreAuthorize("hasAuthority('job:update')")
    public ResponseEntity<JobResponseDTO> archiveJob(@PathVariable UUID publicId) {
        return ResponseEntity.ok(jobService.archiveJob(publicId));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('job:delete')")
    public ResponseEntity<Void> deleteJobByPublicId(@PathVariable UUID publicId) {
        jobService.deleteJobByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }
}
