package com.jesa.interviewslotmanager.dto.authentication;

import com.jesa.interviewslotmanager.dto.user.UserResponseDTO;
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
    // SECURITY: the access/refresh tokens are intentionally NOT exposed in the
    // response body. They are delivered exclusively as httpOnly cookies so that
    // browser JavaScript (and therefore any XSS payload) cannot read them.
    // Left commented out for local debugging only — do NOT re-enable in production.
//    private String accessToken;
//    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserResponseDTO user;
}
