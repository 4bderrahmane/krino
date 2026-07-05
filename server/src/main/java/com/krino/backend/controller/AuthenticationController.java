package com.krino.backend.controller;

import com.krino.backend.dto.authentication.AuthenticationResponseDTO;
import com.krino.backend.dto.authentication.ForgotPasswordRequestDTO;
import com.krino.backend.dto.authentication.RegistrationResponseDTO;
import com.krino.backend.dto.authentication.ResetPasswordRequestDTO;
import com.krino.backend.dto.user.UserLoginDTO;
import com.krino.backend.dto.user.UserRegistrationDTO;
import com.krino.backend.service.AuthenticationService;
import com.krino.backend.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;

    @PostMapping(path = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RegistrationResponseDTO> register(@Valid @RequestPart("data") UserRegistrationDTO request, @RequestPart("resume") MultipartFile resume) {
        RegistrationResponseDTO registration = authenticationService.register(request, resume);
        return ResponseEntity.created(URI.create("/api/users/" + registration.getUser().getId())).body(registration);
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.ok(new CsrfResponse("XSRF-TOKEN", csrfToken.getHeaderName()));
    }

    public record CsrfResponse(String cookieName, String headerName) {}

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseDTO> login(@Valid @RequestBody UserLoginDTO dto, HttpServletResponse response, HttpServletRequest request) {
        return ResponseEntity.ok(authenticationService.login(dto, response, request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authenticationService.logout(request, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponseDTO> refresh(HttpServletRequest request, HttpServletResponse response) {
        AuthenticationResponseDTO authResponse = authenticationService.refresh(request, response);
        return ResponseEntity.ok(authResponse);
    }

    // Always returns 204 regardless of whether the email is registered, so it can't be used to
    // enumerate accounts. A reset link is emailed only when a matching user exists.
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
