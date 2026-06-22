package com.krino.backend.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequestDTO {
    @NotBlank(message = "Department name cannot be blank")
    @Size(max = 100, message = "Department name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
