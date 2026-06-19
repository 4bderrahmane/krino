package com.krino.backend.configuration;

import com.krino.backend.dto.application.ApplicationResponseDTO;
import com.krino.backend.dto.department.DepartmentResponseDTO;
import com.krino.backend.dto.interview.InterviewResponseDTO;
import com.krino.backend.dto.job.JobCreateDTO;
import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.dto.slot.SlotResponseDTO;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.entity.Application;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfiguration
{

    @Bean
    public ModelMapper modelMapper()
    {
        ModelMapper modelMapper = new ModelMapper();

        // The internal Long `id` is never exposed. Every entity -> response DTO mapping
        // sources the DTO's `id` from the entity's public-facing UUID (`publicId`) instead.
        modelMapper.typeMap(Job.class, JobResponseDTO.class)
                .addMappings(m -> m.map(Job::getPublicId, JobResponseDTO::setId));

        modelMapper.typeMap(User.class, UserResponseDTO.class)
                .addMappings(m -> m.map(User::getPublicId, UserResponseDTO::setId));

        modelMapper.typeMap(Department.class, DepartmentResponseDTO.class)
                .addMappings(m -> m.map(Department::getPublicId, DepartmentResponseDTO::setId));

        modelMapper.typeMap(Slot.class, SlotResponseDTO.class)
                .addMappings(m -> m.map(Slot::getPublicId, SlotResponseDTO::setId));

        modelMapper.typeMap(Interview.class, InterviewResponseDTO.class)
                .addMappings(m -> m.map(Interview::getPublicId, InterviewResponseDTO::setId));

        modelMapper.typeMap(Application.class, ApplicationResponseDTO.class)
                .addMappings(m ->
                {
                    m.map(Application::getPublicId, ApplicationResponseDTO::setId);
                    m.map(src -> src.getJob().getPublicId(), ApplicationResponseDTO::setJobId);
                });

        // Only the plain scalar fields (title, description, applyingDeadline) are auto-mapped.
        // department, the enums and status need lookups/validation, so the service sets them.
        // Start from an empty type map so the skips are registered BEFORE implicit mappings run;
        // otherwise ModelMapper would have already deep-mapped `departmentName` -> `department.name`
        // and would then refuse to skip `department`.
        modelMapper.emptyTypeMap(JobCreateDTO.class, Job.class)
                .addMappings(m ->
                {
                    m.skip(Job::setDepartment);
                    m.skip(Job::setEmploymentType);
                    m.skip(Job::setContractType);
                    m.skip(Job::setStatus);
                })
                .implicitMappings();

        return modelMapper;
    }

}
