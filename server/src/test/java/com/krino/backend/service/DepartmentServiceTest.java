package com.krino.backend.service;

import com.krino.backend.dto.department.DepartmentCreateDTO;
import com.krino.backend.dto.department.DepartmentResponseDTO;
import com.krino.backend.dto.department.DepartmentUpdateDTO;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.DepartmentMapper;
import com.krino.backend.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DepartmentServiceTest
{
    private DepartmentRepository departmentRepository;
    private DepartmentMapper departmentMapper;
    private DepartmentService departmentService;

    @BeforeEach
    void setUp()
    {
        departmentRepository = mock(DepartmentRepository.class);
        departmentMapper = mock(DepartmentMapper.class);
        departmentService = new DepartmentService(departmentRepository, departmentMapper);
    }

    @Test
    void createDepartment_uniqueName_mapsSavesAndReturnsResponse()
    {
        DepartmentCreateDTO dto = new DepartmentCreateDTO();
        dto.setName("Engineering");

        Department entity = new Department();
        Department saved = new Department();
        DepartmentResponseDTO response = new DepartmentResponseDTO();

        when(departmentRepository.findByName("Engineering")).thenReturn(Optional.empty());
        when(departmentMapper.toEntity(dto)).thenReturn(entity);
        when(departmentRepository.save(entity)).thenReturn(saved);
        when(departmentMapper.toResponse(saved)).thenReturn(response);

        DepartmentResponseDTO result = departmentService.createDepartment(dto);

        assertThat(result).isSameAs(response);
        verify(departmentRepository).save(entity);
    }

    @Test
    void createDepartment_duplicateName_throwsConflictAndDoesNotSave()
    {
        DepartmentCreateDTO dto = new DepartmentCreateDTO();
        dto.setName("Engineering");

        when(departmentRepository.findByName("Engineering")).thenReturn(Optional.of(new Department()));

        assertThatThrownBy(() -> departmentService.createDepartment(dto))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Engineering");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void updateDepartment_unknownPublicId_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(departmentRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.updateDepartment(publicId, new DepartmentUpdateDTO()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void updateDepartment_renameToExistingDifferentDepartment_throwsConflict()
    {
        UUID publicId = UUID.randomUUID();
        Department existing = new Department();
        existing.setName("Engineering");

        DepartmentUpdateDTO dto = new DepartmentUpdateDTO();
        dto.setName("Finance");

        when(departmentRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(departmentRepository.findByName("Finance")).thenReturn(Optional.of(new Department()));

        assertThatThrownBy(() -> departmentService.updateDepartment(publicId, dto))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Finance");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void updateDepartment_keepingSameName_appliesUpdateAndSaves()
    {
        UUID publicId = UUID.randomUUID();
        Department existing = new Department();
        existing.setName("Engineering");

        DepartmentUpdateDTO dto = new DepartmentUpdateDTO();
        dto.setName("Engineering");

        when(departmentRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(departmentRepository.findByName("Engineering")).thenReturn(Optional.of(existing));
        when(departmentRepository.save(existing)).thenReturn(existing);
        when(departmentMapper.toResponse(existing)).thenReturn(new DepartmentResponseDTO());

        departmentService.updateDepartment(publicId, dto);

        verify(departmentMapper).updateEntity(dto, existing);
        verify(departmentRepository).save(existing);
    }

    @Test
    void patchDepartment_nullName_skipsConflictCheckAndSaves()
    {
        UUID publicId = UUID.randomUUID();
        Department existing = new Department();
        existing.setName("Engineering");

        DepartmentUpdateDTO dto = new DepartmentUpdateDTO();
        dto.setDescription("New description");

        when(departmentRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(existing)).thenReturn(existing);
        when(departmentMapper.toResponse(existing)).thenReturn(new DepartmentResponseDTO());

        departmentService.patchDepartment(publicId, dto);

        verify(departmentRepository, never()).findByName(any());
        verify(departmentMapper).patchEntity(dto, existing);
        verify(departmentRepository).save(existing);
    }

    @Test
    void deleteDepartment_unknownPublicId_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(departmentRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.deleteDepartmentByPublicId(publicId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(departmentRepository, never()).delete(any());
    }

    @Test
    void deleteDepartment_withAttachedJobs_throwsConflictAndDoesNotDelete()
    {
        UUID publicId = UUID.randomUUID();
        Department existing = new Department();
        existing.setName("Engineering");
        existing.setJobs(Set.of(new Job()));

        when(departmentRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> departmentService.deleteDepartmentByPublicId(publicId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("cannot be deleted");

        verify(departmentRepository, never()).delete(any());
    }

    @Test
    void deleteDepartment_withoutJobs_deletesDepartment()
    {
        UUID publicId = UUID.randomUUID();
        Department existing = new Department();
        existing.setName("Engineering");

        when(departmentRepository.findByPublicId(publicId)).thenReturn(Optional.of(existing));

        departmentService.deleteDepartmentByPublicId(publicId);

        verify(departmentRepository).delete(existing);
    }

    @Test
    void getDepartmentByPublicId_unknownPublicId_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(departmentRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getDepartmentByPublicId(publicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
