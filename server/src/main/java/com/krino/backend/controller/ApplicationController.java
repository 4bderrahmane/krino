package com.krino.backend.controller;

import com.krino.backend.dto.application.ApplicationCreateDTO;
import com.krino.backend.dto.application.ApplicationResponseDTO;
import com.krino.backend.dto.application.ApplicationUpdateDTO;
import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_APPLICATION')")
    public ResponseEntity<ApplicationResponseDTO> createApplication(@Valid @RequestBody ApplicationCreateDTO applicationCreateDTO) {
        ApplicationResponseDTO createdApplication = applicationService.createApplication(applicationCreateDTO);
        return ResponseEntity.created(URI.create("/api/applications/" + createdApplication.getId())).body(createdApplication);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_READ_APPLICATION')")
    public ResponseEntity<ApplicationResponseDTO> getApplicationByPublicId(@PathVariable UUID publicId) {
        ApplicationResponseDTO application = applicationService.getApplicationByPublicId(publicId);
        return ResponseEntity.ok(application);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_READ_APPLICATION')")
    public ResponseEntity<PageResponse<ApplicationResponseDTO>> getAllApplications(@PageableDefault(size = 20, sort =
            "id") Pageable pageable) {
        PageResponse<ApplicationResponseDTO> applications = applicationService.getAllApplications(pageable);
        return ResponseEntity.ok(applications);
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_APPLICATION')")
    public ResponseEntity<ApplicationResponseDTO> updateApplication(@PathVariable UUID publicId,
                                                                    @Valid @RequestBody ApplicationUpdateDTO applicationUpdateDTO) {
        ApplicationResponseDTO updatedApplication = applicationService.updateApplication(publicId,
                applicationUpdateDTO);
        return ResponseEntity.ok(updatedApplication);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_APPLICATION')")
    public ResponseEntity<ApplicationResponseDTO> patchApplication(@PathVariable UUID publicId,
                                                                   @RequestBody ApplicationUpdateDTO applicationUpdateDTO) {
        ApplicationResponseDTO patchedApplication = applicationService.patchApplication(publicId, applicationUpdateDTO);
        return ResponseEntity.ok(patchedApplication);
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_DELETE_APPLICATION')")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID publicId) {
        applicationService.deleteApplication(publicId);
        return ResponseEntity.noContent().build();
    }
}
