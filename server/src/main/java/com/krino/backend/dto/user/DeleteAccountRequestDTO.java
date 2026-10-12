package com.krino.backend.dto.user;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequestDTO(
        @NotBlank(message = "Password is required to delete your account") String password) {
}
