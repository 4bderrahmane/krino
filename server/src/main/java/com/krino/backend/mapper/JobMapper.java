package com.krino.backend.mapper;

import com.krino.backend.dto.job.JobCreateDTO;
import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.dto.job.JobUpdateDTO;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.JobStatus;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfiguration.class, uses = DepartmentMapper.class)
public interface JobMapper {
    @Mapping(target = "id", source = "publicId")
    JobResponseDTO toResponse(Job job);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "title", source = "dto.title")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "applyingDeadline", source = "dto.applyingDeadline")
    @Mapping(target = "department", source = "department")
    @Mapping(target = "employmentType", source = "employmentType")
    @Mapping(target = "contractType", source = "contractType")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Job toEntity(JobCreateDTO dto, Department department, EmploymentType employmentType, ContractType contractType,
                 JobStatus status);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "title", source = "dto.title")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "department", source = "department",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "applyingDeadline", ignore = true)
    @Mapping(target = "employmentType", source = "employmentType")
    @Mapping(target = "contractType", source = "contractType")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(JobUpdateDTO dto, Department department, EmploymentType employmentType, ContractType contractType,
                      JobStatus status, @MappingTarget Job job);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "title", source = "dto.title")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "department", source = "department")
    @Mapping(target = "applyingDeadline", ignore = true)
    @Mapping(target = "employmentType", source = "employmentType")
    @Mapping(target = "contractType", source = "contractType")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void patchEntity(JobUpdateDTO dto, Department department, EmploymentType employmentType, ContractType contractType,
                     JobStatus status, @MappingTarget Job job);
}
