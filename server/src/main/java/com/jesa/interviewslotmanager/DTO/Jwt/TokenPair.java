package com.jesa.interviewslotmanager.DTO.Jwt;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class TokenPair
{
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
}
