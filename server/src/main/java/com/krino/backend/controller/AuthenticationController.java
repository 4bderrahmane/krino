package com.krino.backend.controller;

import com.krino.backend.dto.authentication.AuthenticationResponseDTO;
import com.krino.backend.dto.authentication.RegistrationResponseDTO;
import com.krino.backend.dto.user.UserLoginDTO;
import com.krino.backend.dto.user.UserRegistrationDTO;
import com.krino.backend.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController
{
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDTO> register(@Valid @RequestBody UserRegistrationDTO request)
    {
        RegistrationResponseDTO registration = authenticationService.register(request);
        return ResponseEntity.created(URI.create("/api/users/" + registration.getUser().getId())).body(registration);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseDTO> login(@Valid @RequestBody UserLoginDTO request, HttpServletResponse response, HttpServletRequest httpRequest)
    {
        return ResponseEntity.ok(authenticationService.login(request, response, httpRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response)
    {
        authenticationService.logout(request, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponseDTO> refresh(HttpServletRequest request, HttpServletResponse response)
    {
        AuthenticationResponseDTO authResponse = authenticationService.refresh(request, response);
        return ResponseEntity.ok(authResponse);
    }
}
