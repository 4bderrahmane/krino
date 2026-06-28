package com.krino.backend.dto.authentication;

import com.krino.backend.dto.user.UserResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponseDTO {

    private String tokenType;
    private Long expiresIn;
    private UserResponseDTO user;
}
