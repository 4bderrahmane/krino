package com.InterviewManager.interview_slot_manager.DTO.Job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

public class JobRequestDTO {

    @NotNull(message = "Department ID cannot be null")
    private Long departmentId;

    @NotBlank(message = "Title cannot be blank")
    private String title;

    @NotNull(message = "description cannot be null")
    private String description;

}
