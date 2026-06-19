package com.krino.backend.dto.job;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class JobCreateDTO
{
    @NotNull
    private String departmentName;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    @Future
    private LocalDate applyingDeadline;

    @NotNull
    private String employmentType;

    @NotNull
    private String contractType;
}