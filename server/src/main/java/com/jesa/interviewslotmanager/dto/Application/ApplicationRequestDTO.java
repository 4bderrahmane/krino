package com.jesa.interviewslotmanager.dto.Application;


import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class ApplicationRequestDTO {

    @NotNull(message = "Job ID cannot be null")
    private Long jobId;

    @NotNull(message = "Candidate ID cannot be null")
    private Long candidateId;

}
