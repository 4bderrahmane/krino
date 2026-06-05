package com.jesa.interviewslotmanager.controller;

import com.jesa.interviewslotmanager.dto.application.ApplicationCreateDTO;
import com.jesa.interviewslotmanager.dto.application.ApplicationResponseDTO;
import com.jesa.interviewslotmanager.dto.application.ApplicationUpdateDTO;
import com.jesa.interviewslotmanager.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController
{

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationResponseDTO> createApplication(@Valid @RequestBody ApplicationCreateDTO applicationCreateDTO)
    {
        ApplicationResponseDTO createdApplication = applicationService.createApplication(applicationCreateDTO);
        return ResponseEntity.ok(createdApplication);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponseDTO> getApplicationById(@PathVariable Long id)
    {
        ApplicationResponseDTO application = applicationService.getApplicationById(id);
        return ResponseEntity.ok(application);
    }

    @GetMapping
    public ResponseEntity<List<ApplicationResponseDTO>> getAllApplications()
    {
        List<ApplicationResponseDTO> applications = applicationService.getAllApplications();
        return ResponseEntity.ok(applications);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationResponseDTO> updateApplication(@PathVariable Long id, @Valid @RequestBody ApplicationUpdateDTO applicationUpdateDTO)
    {
        ApplicationResponseDTO updatedApplication = applicationService.updateApplication(id, applicationUpdateDTO);
        return ResponseEntity.ok(updatedApplication);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApplicationResponseDTO> patchApplication(@PathVariable Long id, @RequestBody ApplicationUpdateDTO applicationUpdateDTO)
    {
        ApplicationResponseDTO patchedApplication = applicationService.patchApplication(id, applicationUpdateDTO);
        return ResponseEntity.ok(patchedApplication);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteApplication(@PathVariable Long id)
    {
        applicationService.deleteApplication(id);
        return ResponseEntity.ok("Application deleted successfully");
    }
}
