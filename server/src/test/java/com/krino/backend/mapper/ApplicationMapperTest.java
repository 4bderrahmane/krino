package com.krino.backend.mapper;

import com.krino.backend.dto.application.ApplicationCreateDTO;
import com.krino.backend.dto.application.ApplicationUpdateDTO;
import com.krino.backend.entity.Application;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.ApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationMapperTest
{
    private ApplicationMapper applicationMapper;

    @BeforeEach
    void setUp()
    {
        applicationMapper = Mappers.getMapper(ApplicationMapper.class);
    }

    @Test
    void toEntity_assignsJobCandidateResumeAndDefaultsStatusToPending()
    {
        ApplicationCreateDTO dto = new ApplicationCreateDTO(UUID.randomUUID(), "https://cv.example/me.pdf");
        Job job = new Job();
        User candidate = new User();

        Application application = applicationMapper.toEntity(dto, job, candidate);

        assertThat(application.getResumeUrl()).isEqualTo("https://cv.example/me.pdf");
        assertThat(application.getJob()).isSameAs(job);
        assertThat(application.getCandidate()).isSameAs(candidate);
        assertThat(application.getId()).isNull();
        assertThat(application.getPublicId()).isNull();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(application.getAppliedAt()).isNull();
    }

    @Test
    void patchEntity_updatesResumeUrlButIgnoresNullStatus()
    {
        Application existing = new Application();
        existing.setStatus(ApplicationStatus.ACCEPTED);
        existing.setResumeUrl("https://cv.example/old.pdf");

        ApplicationUpdateDTO dto = new ApplicationUpdateDTO();
        dto.setResumeUrl("https://cv.example/new.pdf");

        applicationMapper.patchEntity(dto, null, existing);

        assertThat(existing.getResumeUrl()).isEqualTo("https://cv.example/new.pdf");
        assertThat(existing.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
    }
}
