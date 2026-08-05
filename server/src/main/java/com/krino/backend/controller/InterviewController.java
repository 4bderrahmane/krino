package com.krino.backend.controller;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.interview.InterviewRequestDTO;
import com.krino.backend.dto.interview.InterviewResponseDTO;
import com.krino.backend.service.InterviewService;
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

@Tag(name = "Interviews")
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    private static final SortWhitelist SORT_WHITELIST = SortWhitelist.of(
            "id", "status", "recommendation", "isOnline", "createdDate", "lastModifiedDate");

    @PostMapping
    @PreAuthorize("hasAuthority('interview:create')")
    public ResponseEntity<InterviewResponseDTO> createInterview(@Validated(ValidationGroups.FullUpdate.class)
                                                                @RequestBody InterviewRequestDTO interviewRequestDTO) {
        InterviewResponseDTO createdInterview = interviewService.createInterview(interviewRequestDTO);
        return ResponseEntity.created(URI.create("/api/interviews/" + createdInterview.getId())).body(createdInterview);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('interview:read')")
    public ResponseEntity<InterviewResponseDTO> getInterviewByPublicId(@PathVariable UUID publicId) {
        InterviewResponseDTO interview = interviewService.getInterviewByPublicId(publicId);
        return ResponseEntity.ok(interview);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PageResponse<InterviewResponseDTO>> getAllInterviews(@PageableDefault(size = 20, sort = "id"
    ) Pageable pageable) {
        PageResponse<InterviewResponseDTO> interviews = interviewService.getAllInterviews(SORT_WHITELIST.sanitize(pageable));
        return ResponseEntity.ok(interviews);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('interview:read')")
    public ResponseEntity<PageResponse<InterviewResponseDTO>> getMyInterviews(@PageableDefault(size = 20, sort = "id"
    ) Pageable pageable) {
        PageResponse<InterviewResponseDTO> interviews = interviewService.getMyInterviews(SORT_WHITELIST.sanitize(pageable));
        return ResponseEntity.ok(interviews);
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('interview:update')")
    public ResponseEntity<InterviewResponseDTO> updateInterview(@PathVariable UUID publicId,
                                                                @Validated(ValidationGroups.FullUpdate.class)
                                                                @RequestBody InterviewRequestDTO interviewRequestDTO) {
        InterviewResponseDTO updatedInterview = interviewService.updateInterview(publicId, interviewRequestDTO);
        return ResponseEntity.ok(updatedInterview);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('interview:update')")
    public ResponseEntity<InterviewResponseDTO> patchInterview(@PathVariable UUID publicId,
                                                               @Valid @RequestBody InterviewRequestDTO interviewRequestDTO) {
        InterviewResponseDTO patchedInterview = interviewService.patchInterview(publicId, interviewRequestDTO);
        return ResponseEntity.ok(patchedInterview);
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('interview:delete')")
    public ResponseEntity<Void> deleteInterview(@PathVariable UUID publicId) {
        interviewService.deleteInterview(publicId);
        return ResponseEntity.noContent().build();
    }
}
