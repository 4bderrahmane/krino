package com.jesa.interviewslotmanager.controller;

import com.jesa.interviewslotmanager.dto.application.ApplicationCreateDTO;
import com.jesa.interviewslotmanager.entity.Application;
import com.jesa.interviewslotmanager.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/applications")
public class ApplicationController
{
    private final ApplicationService applicationService;

    @PostMapping("/create")
    public ResponseEntity<Application> createApplication(@Valid @RequestBody ApplicationCreateDTO request)
    {
        Application application = applicationService.createApplication(request);
        return ResponseEntity.ok(application);
    }
}
