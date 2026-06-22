package com.krino.backend.mapper;

import com.krino.backend.dto.department.DepartmentCreateDTO;
import com.krino.backend.dto.department.DepartmentResponseDTO;
import com.krino.backend.dto.department.DepartmentUpdateDTO;
import com.krino.backend.entity.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DepartmentMapperTest
{
    private DepartmentMapper departmentMapper;

    @BeforeEach
    void setUp()
    {
        departmentMapper = Mappers.getMapper(DepartmentMapper.class);
    }

    @Test
    void toResponse_exposesPublicIdAsId()
    {
        UUID publicId = UUID.randomUUID();
        Department department = new Department();
        department.setId(42L);
        department.setPublicId(publicId);
        department.setName("Engineering");
        department.setDescription("Builds things");

        DepartmentResponseDTO response = departmentMapper.toResponse(department);

        assertThat(response.getId()).isEqualTo(publicId);
        assertThat(response.getName()).isEqualTo("Engineering");
        assertThat(response.getDescription()).isEqualTo("Builds things");
    }

    @Test
    void toEntity_copiesScalarsAndLeavesIdentitiesUnset()
    {
        DepartmentCreateDTO dto = new DepartmentCreateDTO();
        dto.setName("Finance");
        dto.setDescription("Money");

        Department entity = departmentMapper.toEntity(dto);

        assertThat(entity.getName()).isEqualTo("Finance");
        assertThat(entity.getDescription()).isEqualTo("Money");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getPublicId()).isNull();
        assertThat(entity.getJobs()).isNull();
    }

    @Test
    void updateEntity_overwritesAllScalarFields()
    {
        Department existing = new Department();
        existing.setName("Engineering");
        existing.setDescription("Old description");

        DepartmentUpdateDTO dto = new DepartmentUpdateDTO();
        dto.setName("Platform");
        dto.setDescription("New description");

        departmentMapper.updateEntity(dto, existing);

        assertThat(existing.getName()).isEqualTo("Platform");
        assertThat(existing.getDescription()).isEqualTo("New description");
    }

    @Test
    void patchEntity_ignoresNullFields()
    {
        Department existing = new Department();
        existing.setName("Engineering");
        existing.setDescription("Old description");

        DepartmentUpdateDTO dto = new DepartmentUpdateDTO();
        dto.setDescription("New description");

        departmentMapper.patchEntity(dto, existing);

        assertThat(existing.getName()).isEqualTo("Engineering");
        assertThat(existing.getDescription()).isEqualTo("New description");
    }
}
