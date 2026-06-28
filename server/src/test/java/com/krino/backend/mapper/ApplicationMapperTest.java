package com.krino.backend.mapper;

import com.krino.backend.dto.application.ApplicationCreateDTO;
import com.krino.backend.dto.application.ApplicationResponseDTO;
import com.krino.backend.dto.application.ApplicationUpdateDTO;
import com.krino.backend.entity.Application;
import com.krino.backend.entity.Job;
import com.krino.backend.support.TestJobs;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.ApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationMapperTest
{
    private ApplicationMapper applicationMapper;

    @BeforeEach
    void setUp()
    {
        applicationMapper = Mappers.getMapper(ApplicationMapper.class);
        ReflectionTestUtils.setField(applicationMapper, "userMapper", Mappers.getMapper(UserMapper.class));
    }

    @Test
    void toEntity_assignsJobCandidateResumeAndDefaultsStatusToPending()
    {
        ApplicationCreateDTO dto = new ApplicationCreateDTO(UUID.randomUUID());
        Job job = TestJobs.draft("Backend Engineer");
        User candidate = new User();

        Application application = applicationMapper.toEntity(dto, job, candidate);

        assertThat(application.getResumeObjectKey()).isNull();
        assertThat(application.getResumeOriginalFilename()).isNull();
        assertThat(application.getJob()).isSameAs(job);
        assertThat(application.getCandidate()).isSameAs(candidate);
        assertThat(application.getId()).isNull();
        // publicId is auto-assigned at construction (stable identity for transient
        // entities); only the DB surrogate id stays unset until persist.
        assertThat(application.getPublicId()).isNotNull();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(application.getAppliedAt()).isNull();
    }

    @Test
    void patchEntity_ignoresResumeStorageFieldsAndNullStatusAndKeepsJob()
    {
        Job job = TestJobs.draft("Backend Engineer");
        Application existing = new Application();
        existing.setStatus(ApplicationStatus.ACCEPTED);
        existing.setResumeObjectKey("applications/app/resume/old.pdf");
        existing.setResumeOriginalFilename("old.pdf");
        existing.setJob(job);

        ApplicationUpdateDTO dto = new ApplicationUpdateDTO();
        dto.setJobId(UUID.randomUUID());

        applicationMapper.patchEntity(dto, existing);

        assertThat(existing.getResumeObjectKey()).isEqualTo("applications/app/resume/old.pdf");
        assertThat(existing.getResumeOriginalFilename()).isEqualTo("old.pdf");
        assertThat(existing.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(existing.getJob()).isSameAs(job);
    }

    @Test
    void toResponse_mapsResumeMetadataWithoutObjectKey()
    {
        UUID applicationId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        LocalDateTime uploadedAt = LocalDateTime.of(2026, Month.JANUARY, 15, 10, 30);
        Job job = TestJobs.withPublicId(TestJobs.draft("Backend Engineer"), jobId);
        Application application = new Application();
        application.setPublicId(applicationId);
        application.setJob(job);
        application.setResumeObjectKey("applications/app/resume/cv.pdf");
        application.setResumeOriginalFilename("cv.pdf");
        application.setResumeContentType("application/pdf");
        application.setResumeSizeBytes(2048L);
        application.setResumeUploadedAt(uploadedAt);

        ApplicationResponseDTO response = applicationMapper.toResponse(application);

        assertThat(response.getId()).isEqualTo(applicationId);
        assertThat(response.getJobId()).isEqualTo(jobId);
        assertThat(response.getResume()).isNotNull();
        assertThat(response.getResume().getOriginalFilename()).isEqualTo("cv.pdf");
        assertThat(response.getResume().getContentType()).isEqualTo("application/pdf");
        assertThat(response.getResume().getSizeBytes()).isEqualTo(2048L);
        assertThat(response.getResume().getUploadedAt()).isEqualTo(uploadedAt);
    }
}
