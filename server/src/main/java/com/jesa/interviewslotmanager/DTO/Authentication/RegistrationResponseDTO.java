package com.jesa.interviewslotmanager.DTO.Authentication;

import com.jesa.interviewslotmanager.DTO.User.UserResponseDTO;
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