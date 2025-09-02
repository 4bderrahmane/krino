package com.jesa.interviewslotmanager.DTO.Authentication;

import com.jesa.interviewslotmanager.DTO.User.UserResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponseDTO
{
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserResponseDTO user;
}
