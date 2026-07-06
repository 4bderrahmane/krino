package com.krino.backend.controller;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.interview.InterviewRequestDTO;
import com.krino.backend.dto.interview.InterviewResponseDTO;
import com.krino.backend.service.InterviewService;
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

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_INTERVIEW')")
    public ResponseEntity<InterviewResponseDTO> createInterview(@Validated(ValidationGroups.FullUpdate.class)
                                                                @RequestBody InterviewRequestDTO interviewRequestDTO) {
        InterviewResponseDTO createdInterview = interviewService.createInterview(interviewRequestDTO);
        return ResponseEntity.created(URI.create("/api/interviews/" + createdInterview.getId())).body(createdInterview);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_READ_INTERVIEW')")
    public ResponseEntity<InterviewResponseDTO> getInterviewByPublicId(@PathVariable UUID publicId) {
        InterviewResponseDTO interview = interviewService.getInterviewByPublicId(publicId);
        return ResponseEntity.ok(interview);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PageResponse<InterviewResponseDTO>> getAllInterviews(@PageableDefault(size = 20, sort = "id"
    ) Pageable pageable) {
        PageResponse<InterviewResponseDTO> interviews = interviewService.getAllInterviews(pageable);
        return ResponseEntity.ok(interviews);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('CAN_READ_INTERVIEW')")
    public ResponseEntity<PageResponse<InterviewResponseDTO>> getMyInterviews(@PageableDefault(size = 20, sort = "id"
    ) Pageable pageable) {
        PageResponse<InterviewResponseDTO> interviews = interviewService.getMyInterviews(pageable);
        return ResponseEntity.ok(interviews);
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_INTERVIEW')")
    public ResponseEntity<InterviewResponseDTO> updateInterview(@PathVariable UUID publicId,
                                                                @Validated(ValidationGroups.FullUpdate.class)
                                                                @RequestBody InterviewRequestDTO interviewRequestDTO) {
        InterviewResponseDTO updatedInterview = interviewService.updateInterview(publicId, interviewRequestDTO);
        return ResponseEntity.ok(updatedInterview);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_INTERVIEW')")
    public ResponseEntity<InterviewResponseDTO> patchInterview(@PathVariable UUID publicId,
                                                               @Valid @RequestBody InterviewRequestDTO interviewRequestDTO) {
        InterviewResponseDTO patchedInterview = interviewService.patchInterview(publicId, interviewRequestDTO);
        return ResponseEntity.ok(patchedInterview);
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_DELETE_INTERVIEW')")
    public ResponseEntity<Void> deleteInterview(@PathVariable UUID publicId) {
        interviewService.deleteInterview(publicId);
        return ResponseEntity.noContent().build();
    }
}
