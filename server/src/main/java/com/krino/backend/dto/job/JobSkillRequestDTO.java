package com.krino.backend.dto.job;

import com.krino.backend.entity.enums.SkillImportance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JobSkillRequestDTO {
    @NotBlank(message = "Skill name cannot be blank")
    @Pattern(regexp = ".*[\\p{L}\\p{N}+#].*", message = "Skill name must include letters, numbers, +, or #")
    @Size(max = 100, message = "Skill name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Skill importance cannot be null")
    private SkillImportance importance = SkillImportance.REQUIRED;
}
