package com.krino.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdatePasswordDTO {

    @NotBlank(message = "Current password cannot be blank")
    private String currentPassword;

    @NotBlank(message = "New password cannot be blank")
    @Size(min = 6, max = 100)
    private String newPassword;

    @NotBlank(message = "New password confirmation cannot be blank")
    @Size(min = 6, max = 100)
    private String confirmNewPassword;
}
