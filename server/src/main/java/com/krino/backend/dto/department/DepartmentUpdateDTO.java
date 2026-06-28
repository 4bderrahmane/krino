package com.krino.backend.dto.department;

import com.krino.backend.validation.ValidationGroups;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.Data;

@Data
public class DepartmentUpdateDTO {
    @NotNull(message = "Department name cannot be null", groups = ValidationGroups.FullUpdate.class)
    @Pattern(regexp = "(?s).*\\S.*", message = "Department name cannot be blank",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    @Size(max = 100, message = "Department name cannot exceed 100 characters",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    private String name;

    @Pattern(regexp = "(?s).*\\S.*", message = "Description cannot be blank",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    @Size(max = 255, message = "Description cannot exceed 255 characters",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    private String description;
}
