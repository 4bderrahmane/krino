package com.krino.backend.controller;

import com.krino.backend.dto.application.ApplicationCreateDTO;
import com.krino.backend.dto.application.ApplicationResponseDTO;
import com.krino.backend.dto.application.ApplicationUpdateDTO;
import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.service.ApplicationService;
import com.krino.backend.utility.SortWhitelist;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Tag(name = "Applications")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    private static final SortWhitelist SORT_WHITELIST = SortWhitelist.of(
            "id", "status", "appliedAt", "createdDate", "lastModifiedDate");

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
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PageResponse<ApplicationResponseDTO>> getAllApplications(@PageableDefault(size = 20, sort =
            "id") Pageable pageable) {
        PageResponse<ApplicationResponseDTO> applications = applicationService.getAllApplications(SORT_WHITELIST.sanitize(pageable));
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('CAN_READ_APPLICATION')")
    public ResponseEntity<PageResponse<ApplicationResponseDTO>> getMyApplications(@PageableDefault(size = 20, sort =
            "id") Pageable pageable) {
        PageResponse<ApplicationResponseDTO> applications = applicationService.getMyApplications(SORT_WHITELIST.sanitize(pageable));
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
                                                                   @Valid @RequestBody ApplicationUpdateDTO applicationUpdateDTO) {
        ApplicationResponseDTO patchedApplication = applicationService.patchApplication(publicId, applicationUpdateDTO);
        return ResponseEntity.ok(patchedApplication);
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_DELETE_APPLICATION')")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID publicId) {
        applicationService.deleteApplication(publicId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(path = "/{publicId}/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CAN_UPDATE_APPLICATION')")
    public ResponseEntity<ApplicationResponseDTO> uploadResume(@PathVariable UUID publicId,
                                                               @RequestPart("resume") MultipartFile resume) {
        ApplicationResponseDTO application = applicationService.uploadResume(publicId, resume);
        return ResponseEntity.ok(application);
    }

    @PutMapping("/{publicId}/resume/from-base")
    @PreAuthorize("hasAuthority('CAN_UPDATE_APPLICATION')")
    public ResponseEntity<ApplicationResponseDTO> applyBaseResume(@PathVariable UUID publicId) {
        ApplicationResponseDTO application = applicationService.applyBaseResume(publicId);
        return ResponseEntity.ok(application);
    }

    @GetMapping("/{publicId}/resume")
    @PreAuthorize("hasAuthority('CAN_READ_APPLICATION')")
    public ResponseEntity<Resource> downloadResume(@PathVariable UUID publicId) {
        ApplicationService.ResumeDownload resume = applicationService.downloadResume(publicId);
        return getResourceResponseEntity(resume);
    }

    @NonNull
    static ResponseEntity<Resource> getResourceResponseEntity(ApplicationService.ResumeDownload resume) {
        InputStreamResource resource = new InputStreamResource(resume.inputStream());
        MediaType mediaType = MediaType.parseMediaType(resume.contentType());
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(resume.originalFilename(), StandardCharsets.UTF_8)
                .build();

        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

        if (resume.sizeBytes() != null) response.contentLength(resume.sizeBytes());
        return response.body(resource);
    }

    @DeleteMapping("/{publicId}/resume")
    @PreAuthorize("hasAuthority('CAN_UPDATE_APPLICATION')")
    public ResponseEntity<Void> deleteResume(@PathVariable UUID publicId) {
        applicationService.deleteResume(publicId);
        return ResponseEntity.noContent().build();
    }
}
