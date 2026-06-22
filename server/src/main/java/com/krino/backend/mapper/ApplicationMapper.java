package com.krino.backend.mapper;

import com.krino.backend.dto.application.ApplicationCreateDTO;
import com.krino.backend.dto.application.ApplicationResponseDTO;
import com.krino.backend.dto.application.ApplicationUpdateDTO;
import com.krino.backend.entity.Application;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfiguration.class, uses = UserMapper.class)
public interface ApplicationMapper {
    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "jobId", source = "job.publicId")
    ApplicationResponseDTO toResponse(Application application);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "resumeUrl", source = "dto.resumeUrl")
    @Mapping(target = "job", source = "job")
    @Mapping(target = "candidate", source = "candidate")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Application toEntity(ApplicationCreateDTO dto, Job job, User candidate);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "job", source = "job",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "candidate", ignore = true)
    @Mapping(target = "resumeUrl", source = "dto.resumeUrl")
    @Mapping(target = "status", source = "dto.status",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ApplicationUpdateDTO dto, Job job, @MappingTarget Application application);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "job", source = "job")
    @Mapping(target = "candidate", ignore = true)
    @Mapping(target = "resumeUrl", source = "dto.resumeUrl")
    @Mapping(target = "status", source = "dto.status")
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void patchEntity(ApplicationUpdateDTO dto, Job job, @MappingTarget Application application);
}
