package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.department.DepartmentCreateDTO;
import com.krino.backend.dto.department.DepartmentResponseDTO;
import com.krino.backend.dto.department.DepartmentUpdateDTO;
import com.krino.backend.entity.Department;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.DepartmentMapper;
import com.krino.backend.repository.DepartmentRepository;
import com.krino.backend.utility.ErrorCode;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

import static com.krino.backend.configuration.CachingConfiguration.DEPARTMENTS_CACHE;
import static com.krino.backend.configuration.CachingConfiguration.JOBS_CACHE;
import static com.krino.backend.configuration.CachingConfiguration.JOB_LISTINGS_CACHE;

@Service
@AllArgsConstructor
@Transactional
public class DepartmentService {
    public static final String RESOURCE = Department.class.getSimpleName();
    private static final String DEPARTMENT_ALREADY_EXISTS = "Department '%s' already exists";
    public static final String PUBLIC_ID = "publicId";
    public static final String FIELD = "field";
    public static final String NAME = "name";
    public static final String VALUE = "value";
    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    // Deletion is only allowed for departments without jobs, so the job caches can't hold it.
    @CacheEvict(cacheNames = DEPARTMENTS_CACHE, allEntries = true)
    public void deleteDepartmentByPublicId(UUID publicId) {
        Department department = departmentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, PUBLIC_ID, publicId));

        if (department.getJobs() != null && !department.getJobs().isEmpty()) {
            throw new ResourceConflictException(
                    String.format("Department '%s' still has jobs and cannot be deleted.", department.getName()),
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of("resource", "Department", "name", department.getName()));
        }

        departmentRepository.delete(department);
    }

    @CacheEvict(cacheNames = DEPARTMENTS_CACHE, allEntries = true)
    public DepartmentResponseDTO createDepartment(DepartmentCreateDTO department) {
        if (departmentRepository.findByName(department.getName()).isPresent()) {
            throw new ResourceConflictException(String.format(DEPARTMENT_ALREADY_EXISTS, department.getName()),
                    ErrorCode.DATA_CONFLICT,
                    Map.of(FIELD, NAME, VALUE, department.getName()));
        }
        Department newDepartment = departmentMapper.toEntity(department);
        Department savedDepartment = departmentRepository.save(newDepartment);

        return departmentMapper.toResponse(savedDepartment);
    }

    // Job response DTOs embed the department, so a rename must also flush the job caches.
    @Caching(evict = {
            @CacheEvict(cacheNames = DEPARTMENTS_CACHE, allEntries = true),
            @CacheEvict(cacheNames = JOBS_CACHE, allEntries = true),
            @CacheEvict(cacheNames = JOB_LISTINGS_CACHE, allEntries = true)})
    public DepartmentResponseDTO updateDepartment(UUID publicId, DepartmentUpdateDTO departmentUpdateDTO) {
        Department existingDepartment = departmentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, PUBLIC_ID, publicId));

        if (departmentRepository.findByName(departmentUpdateDTO.getName()).isPresent() && !existingDepartment.getName().equals(departmentUpdateDTO.getName())) {
            throw new ResourceConflictException(String.format(DEPARTMENT_ALREADY_EXISTS,
                    departmentUpdateDTO.getName()), ErrorCode.DATA_CONFLICT,
                    Map.of(FIELD, NAME, VALUE, departmentUpdateDTO.getName()));
        }

        departmentMapper.updateEntity(departmentUpdateDTO, existingDepartment);

        Department updatedDepartment = departmentRepository.save(existingDepartment);
        return departmentMapper.toResponse(updatedDepartment);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = DEPARTMENTS_CACHE, allEntries = true),
            @CacheEvict(cacheNames = JOBS_CACHE, allEntries = true),
            @CacheEvict(cacheNames = JOB_LISTINGS_CACHE, allEntries = true)})
    public DepartmentResponseDTO patchDepartment(UUID publicId, DepartmentUpdateDTO dto) {
        Department existingDepartment = departmentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, PUBLIC_ID, publicId));

        if (dto.getName() != null && departmentRepository.findByName(dto.getName()).isPresent() && !existingDepartment.getName().equals(dto.getName())) {
            throw new ResourceConflictException(String.format(DEPARTMENT_ALREADY_EXISTS,
                    dto.getName()), ErrorCode.DATA_CONFLICT,
                    Map.of(FIELD, NAME, VALUE, dto.getName()));
        }


        departmentMapper.patchEntity(dto, existingDepartment);

        Department updatedDepartment = departmentRepository.save(existingDepartment);
        return departmentMapper.toResponse(updatedDepartment);
    }

    public DepartmentResponseDTO getDepartmentByPublicId(UUID publicId) {
        Department department = departmentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, PUBLIC_ID, publicId));
        return departmentMapper.toResponse(department);
    }

    // Unlike jobs, this endpoint really paginates, so the key carries the page coordinates.
    @Cacheable(cacheNames = DEPARTMENTS_CACHE,
            key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public PageResponse<DepartmentResponseDTO> getAllDepartments(Pageable pageable) {
        return PageResponse.from(departmentRepository.findAll(pageable), departmentMapper::toResponse);
    }
}
