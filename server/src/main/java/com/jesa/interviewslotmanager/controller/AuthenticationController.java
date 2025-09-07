package com.jesa.interviewslotmanager.controller;

import com.jesa.interviewslotmanager.dto.Authentication.AuthenticationResponseDTO;
import com.jesa.interviewslotmanager.dto.Authentication.RegistrationResponseDTO;
import com.jesa.interviewslotmanager.dto.User.UserLoginDTO;
import com.jesa.interviewslotmanager.dto.User.UserRegistrationDTO;
import com.jesa.interviewslotmanager.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController
{
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDTO> register(@RequestBody UserRegistrationDTO request)
    {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseDTO> login(@RequestBody UserLoginDTO request, HttpServletResponse response, HttpServletRequest httpRequest)
    {
        return ResponseEntity.ok(authenticationService.login(request, response, httpRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response)
    {
        return ResponseEntity.ok(authenticationService.logout(request, response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponseDTO> refresh(HttpServletRequest request, HttpServletResponse response)
    {
        try
        {
            AuthenticationResponseDTO authResponse = authenticationService.refresh(request, response);
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e)
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
