package com.jesa.interviewslotmanager.dto.job;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

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
    private Date applyingDeadline;

    @NotNull
    private String type;
}