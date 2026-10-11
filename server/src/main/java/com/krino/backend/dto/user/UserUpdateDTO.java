package com.krino.backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDTO {

    @NotNull
    private Long version;

    @Size(min = 2, max = 50)
    private String firstName;

    @Size(min = 2, max = 50)
    private String lastName;

    @Pattern(regexp = "(?s).*\\S.*", message = "Email must not be blank")
    @Email
    private String email;

    @Pattern(regexp = "\\d{9}", message = "Phone number must be exactly 9 digits")
    private String phoneNumber;
}
