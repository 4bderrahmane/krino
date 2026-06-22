package com.krino.backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class UserLoginDTO {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
