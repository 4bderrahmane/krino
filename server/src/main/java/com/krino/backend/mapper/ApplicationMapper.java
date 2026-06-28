package com.krino.backend.mapper;

import com.krino.backend.dto.application.ApplicationCreateDTO;
import com.krino.backend.dto.application.ApplicationResumeDTO;
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
    @Mapping(target = "jobTitle", source = "job.title")
    @Mapping(target = "jobDepartment", source = "job.department.name")
    @Mapping(target = "resume", source = "application")
    ApplicationResponseDTO toResponse(Application application);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "job", source = "job")
    @Mapping(target = "candidate", source = "candidate")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "resumeObjectKey", ignore = true)
    @Mapping(target = "resumeOriginalFilename", ignore = true)
    @Mapping(target = "resumeContentType", ignore = true)
    @Mapping(target = "resumeSizeBytes", ignore = true)
    @Mapping(target = "resumeUploadedAt", ignore = true)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    Application toEntity(ApplicationCreateDTO dto, Job job, User candidate);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "job", ignore = true)
    @Mapping(target = "candidate", ignore = true)
    @Mapping(target = "resumeObjectKey", ignore = true)
    @Mapping(target = "resumeOriginalFilename", ignore = true)
    @Mapping(target = "resumeContentType", ignore = true)
    @Mapping(target = "resumeSizeBytes", ignore = true)
    @Mapping(target = "resumeUploadedAt", ignore = true)
    @Mapping(target = "status", source = "dto.status",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    void updateEntity(ApplicationUpdateDTO dto, @MappingTarget Application application);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "job", ignore = true)
    @Mapping(target = "candidate", ignore = true)
    @Mapping(target = "resumeObjectKey", ignore = true)
    @Mapping(target = "resumeOriginalFilename", ignore = true)
    @Mapping(target = "resumeContentType", ignore = true)
    @Mapping(target = "resumeSizeBytes", ignore = true)
    @Mapping(target = "resumeUploadedAt", ignore = true)
    @Mapping(target = "status", source = "dto.status")
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    void patchEntity(ApplicationUpdateDTO dto, @MappingTarget Application application);

    default ApplicationResumeDTO toResume(Application application) {
        if (application == null || application.getResumeObjectKey() == null || application.getResumeObjectKey().isBlank()) {
            return null;
        }
        return new ApplicationResumeDTO(
                application.getResumeOriginalFilename(),
                application.getResumeContentType(),
                application.getResumeSizeBytes(),
                application.getResumeUploadedAt());
    }
}
