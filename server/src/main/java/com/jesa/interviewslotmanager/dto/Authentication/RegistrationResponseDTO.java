package com.jesa.interviewslotmanager.dto.Authentication;

import com.jesa.interviewslotmanager.dto.User.UserResponseDTO;
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