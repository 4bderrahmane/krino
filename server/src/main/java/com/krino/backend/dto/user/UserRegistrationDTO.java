package com.krino.backend.dto.user;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class UserRegistrationDTO
{

    @NotBlank(message = "First name cannot be blank")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name cannot be blank")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be a valid email format")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, max = 255, message = "Password must be at least 8 characters long")
    private String password;

    @Size(min = 9, max = 9, message = "Phone number must be 9 digits")
    private String phoneNumber;
}
