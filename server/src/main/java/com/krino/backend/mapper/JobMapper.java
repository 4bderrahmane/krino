package com.krino.backend.mapper;

import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.dto.job.JobSkillResponseDTO;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.JobSkill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Read-only mapper. Job is a rich aggregate with no setters, so it cannot be
 * populated field-by-field: creation and updates go through the constructor and
 * its behaviour methods in the service layer. The mapper only renders responses.
 */
@Mapper(config = MapperConfiguration.class, uses = DepartmentMapper.class)
public interface JobMapper {

    @Mapping(target = "id", source = "publicId")
    JobResponseDTO toResponse(Job job);

    default List<JobSkillResponseDTO> mapSkills(Set<JobSkill> skills) {
        if (skills == null) {
            return List.of();
        }

        return skills.stream()
                .filter(jobSkill -> jobSkill.getSkill() != null)
                .sorted(Comparator
                        .comparing(JobSkill::getImportance)
                        .thenComparing(jobSkill -> jobSkill.getSkill().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(this::toSkillResponse)
                .toList();
    }

    default JobSkillResponseDTO toSkillResponse(JobSkill jobSkill) {
        JobSkillResponseDTO response = new JobSkillResponseDTO();
        response.setName(jobSkill.getSkill().getName());
        response.setSlug(jobSkill.getSkill().getSlug());
        response.setImportance(jobSkill.getImportance());
        return response;
    }
}
