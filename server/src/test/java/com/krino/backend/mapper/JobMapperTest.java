package com.krino.backend.mapper;

import com.krino.backend.dto.job.JobCreateDTO;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JobMapperTest {
    private JobMapper jobMapper;

    @BeforeEach
    void setUp() {
        jobMapper = Mappers.getMapper(JobMapper.class);
    }

    @Test
    void jobCreateDto_mapsScalarFieldsOntoJob() {
        JobCreateDTO dto = new JobCreateDTO();
        dto.setTitle("Backend Engineer");
        dto.setDescription("Build APIs");
        dto.setApplyingDeadline(LocalDate.of(2026, 12, 31));

        Job job = jobMapper.toEntity(dto, new Department(), EmploymentType.FULL_TIME, ContractType.PERMANENT,
                JobStatus.OPEN);

        assertEquals("Backend Engineer", job.getTitle());
        assertEquals("Build APIs", job.getDescription());
        assertEquals(LocalDate.of(2026, 12, 31), job.getApplyingDeadline());
    }

    @Test
    void jobCreateDto_doesNotTouchServiceOwnedFields() {
        JobCreateDTO dto = new JobCreateDTO();
        dto.setTitle("Backend Engineer");
        dto.setDepartmentName("Engineering");
        dto.setEmploymentType("FULL_TIME");
        dto.setContractType("PERMANENT");

        Job job = jobMapper.toEntity(dto, null, null, null, JobStatus.OPEN);

        assertNull(job.getDepartment());
        assertNull(job.getEmploymentType());
        assertNull(job.getContractType());
        assertEquals(JobStatus.OPEN, job.getStatus());
    }
}
