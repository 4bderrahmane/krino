package com.krino.backend.configuration;

import com.krino.backend.dto.job.JobCreateDTO;
import com.krino.backend.entity.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModelMapperConfigurationTest
{
    private ModelMapper modelMapper;

    @BeforeEach
    void setUp()
    {
        modelMapper = new ModelMapperConfiguration().modelMapper();
    }

    @Test
    void jobCreateDto_mapsScalarFieldsOntoJob()
    {
        JobCreateDTO dto = new JobCreateDTO();
        dto.setTitle("Backend Engineer");
        dto.setDescription("Build APIs");
        dto.setApplyingDeadline(LocalDate.of(2026, 12, 31));

        Job job = new Job();
        modelMapper.map(dto, job);

        assertEquals("Backend Engineer", job.getTitle());
        assertEquals("Build APIs", job.getDescription());
        assertEquals(LocalDate.of(2026, 12, 31), job.getApplyingDeadline());
    }

    @Test
    void jobCreateDto_doesNotTouchServiceOwnedFields()
    {
        // department, the enums and status are skipped in the type map; the service sets them.
        // In particular `departmentName` must NOT be deep-mapped onto department.name.
        JobCreateDTO dto = new JobCreateDTO();
        dto.setTitle("Backend Engineer");
        dto.setDepartmentName("Engineering");
        dto.setEmploymentType("FULL_TIME");
        dto.setContractType("PERMANENT");

        Job job = new Job();
        job.setStatus(null);
        modelMapper.map(dto, job);

        assertNull(job.getDepartment());
        assertNull(job.getEmploymentType());
        assertNull(job.getContractType());
        assertNull(job.getStatus());
    }
}
