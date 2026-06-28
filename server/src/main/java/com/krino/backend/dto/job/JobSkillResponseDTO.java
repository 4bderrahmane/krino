package com.krino.backend.dto.job;

import com.krino.backend.entity.enums.SkillImportance;
import lombok.Data;

@Data
public class JobSkillResponseDTO {
    private String name;
    private String slug;
    private SkillImportance importance;
}
