package com.krino.backend.controller;

import com.krino.backend.dto.interview.InterviewRequestDTO;
import com.krino.backend.dto.interview.InterviewResponseDTO;
import com.krino.backend.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController
{

    private final InterviewService interviewService;

    @PostMapping("/create")
    public ResponseEntity<InterviewResponseDTO> createInterview(@Valid @RequestBody InterviewRequestDTO interviewRequestDTO)
    {
        InterviewResponseDTO createdInterview = interviewService.createInterview(interviewRequestDTO);
        return ResponseEntity.ok(createdInterview);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponseDTO> getInterviewById(@PathVariable Long id)
    {
        InterviewResponseDTO interview = interviewService.getInterviewById(id);
        return ResponseEntity.ok(interview);
    }

    @GetMapping
    public ResponseEntity<List<InterviewResponseDTO>> getAllInterviews()
    {
        List<InterviewResponseDTO> interviews = interviewService.getAllInterviews();
        return ResponseEntity.ok(interviews);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InterviewResponseDTO> updateInterview(@PathVariable Long id, @Valid @RequestBody InterviewRequestDTO interviewRequestDTO)
    {
        InterviewResponseDTO updatedInterview = interviewService.updateInterview(id, interviewRequestDTO);
        return ResponseEntity.ok(updatedInterview);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<InterviewResponseDTO> patchInterview(@PathVariable Long id, @RequestBody InterviewRequestDTO interviewRequestDTO)
    {
        InterviewResponseDTO patchedInterview = interviewService.patchInterview(id, interviewRequestDTO);
        return ResponseEntity.ok(patchedInterview);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInterview(@PathVariable Long id)
    {
        interviewService.deleteInterview(id);
        return ResponseEntity.noContent().build();
    }
}
