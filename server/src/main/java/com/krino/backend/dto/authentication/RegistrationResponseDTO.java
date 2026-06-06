package com.krino.backend.dto.authentication;

import com.krino.backend.dto.user.UserResponseDTO;
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