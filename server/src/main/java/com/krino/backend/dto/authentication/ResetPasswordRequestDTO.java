package com.krino.backend.dto.authentication;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequestDTO {

    @NotBlank(message = "Token cannot be blank")
    private String token;

    @NotBlank(message = "New password cannot be blank")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
    private String newPassword;

    @NotBlank(message = "New password confirmation cannot be blank")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
    private String confirmNewPassword;

    @AssertTrue(message = "New password and confirmation do not match")
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(confirmNewPassword);
    }
}
