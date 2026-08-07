package com.krino.backend.controller;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.service.JobService;
import com.krino.backend.utility.SortWhitelist;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Jobs (public)")
@SecurityRequirements
@RestController
@RequestMapping("/api/public/jobs")
@RequiredArgsConstructor
public class PublicJobController {

    private final JobService jobService;

    private static final SortWhitelist SORT_WHITELIST = SortWhitelist.of(
            "id", "title", "publishedAt", "applicationDeadline");

    @GetMapping
    public ResponseEntity<PageResponse<JobResponseDTO>> getOpenJobs(@PageableDefault(size = 20, sort = "publishedAt") Pageable pageable) {
        return ResponseEntity.ok(jobService.getOpenJobs(SORT_WHITELIST.sanitize(pageable)));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<JobResponseDTO> getOpenJobByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(jobService.getPublicJobByPublicId(publicId));
    }
}
