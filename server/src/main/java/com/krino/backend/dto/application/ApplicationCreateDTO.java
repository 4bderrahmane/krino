package com.krino.backend.dto.application;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreateDTO {
    @NotNull(message = "Job ID cannot be null")
    private UUID jobId;
}
