package com.jesa.interviewslotmanager.dto.authentication;

import com.jesa.interviewslotmanager.dto.user.UserResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationResponseDTO
{
    private UserResponseDTO user;
    private String message;
}