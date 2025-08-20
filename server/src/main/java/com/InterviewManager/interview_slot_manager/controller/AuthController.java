package com.InterviewManager.interview_slot_manager.controller;

import com.InterviewManager.interview_slot_manager.DTO.Authentication.AuthenticationResponseDTO;
import com.InterviewManager.interview_slot_manager.DTO.Authentication.RegistrationResponseDTO;
import com.InterviewManager.interview_slot_manager.DTO.User.UserLoginDTO;
import com.InterviewManager.interview_slot_manager.DTO.User.UserRegistrationDTO;
import com.InterviewManager.interview_slot_manager.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController
{

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDTO> register(@RequestBody UserRegistrationDTO request)
    {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseDTO> login(@RequestBody UserLoginDTO request)
    {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token)
    {
        return ResponseEntity.ok(authService.logout(token));
    }
}
