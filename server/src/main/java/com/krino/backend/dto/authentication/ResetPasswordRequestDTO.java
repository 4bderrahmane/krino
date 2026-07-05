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
    @Size(min = 8, max = 255, message = "Password must be at least 8 characters long")
    private String newPassword;

    @NotBlank(message = "New password confirmation cannot be blank")
    private String confirmNewPassword;

    // Validated here (a 400) rather than in the service, so a mismatch never surfaces as a 401
    // that would bounce the unauthenticated reset page to login.
    @AssertTrue(message = "New password and confirmation do not match")
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(confirmNewPassword);
    }
}
