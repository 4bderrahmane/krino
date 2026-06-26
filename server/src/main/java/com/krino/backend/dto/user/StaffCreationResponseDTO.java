package com.krino.backend.dto.user;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StaffCreationResponseDTO {
    private UserResponseDTO user;
    private String initialPassword;
}
