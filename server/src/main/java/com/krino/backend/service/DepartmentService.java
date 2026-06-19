package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.department.DepartmentCreateDTO;
import com.krino.backend.dto.department.DepartmentResponseDTO;
import com.krino.backend.dto.department.DepartmentUpdateDTO;
import com.krino.backend.entity.Department;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.repository.DepartmentRepository;
import com.krino.backend.utility.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class DepartmentService
{
    private static final String DEPARTMENT_ALREADY_EXISTS = "Department '%s' already exists";
    private final DepartmentRepository departmentRepository;
    private final ModelMapper modelMapper;

    public void deleteDepartmentByPublicId(UUID publicId)
    {
        // use object-name constructor to get DEPARTMENT_NOT_FOUND ErrorCode
        Department department = departmentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Department.class.getSimpleName(), "publicId", publicId));

        if (department.getJobs() != null && !department.getJobs().isEmpty())
        {
            throw new ResourceConflictException(
                    String.format("Department '%s' still has jobs and cannot be deleted.", department.getName()),
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of("resource", "Department", "name", department.getName()));
        }

        departmentRepository.delete(department);
    }

    public DepartmentResponseDTO createDepartment(DepartmentCreateDTO department)
    {
        if (departmentRepository.findByName(department.getName()).isPresent())
        {
            throw new ResourceConflictException(String.format(DEPARTMENT_ALREADY_EXISTS, department.getName()), ErrorCode.DATA_CONFLICT,
                    Map.of("field", "name", "value", department.getName()));
        }
        Department newDepartment = new Department();

        newDepartment.setName(department.getName());
        newDepartment.setDescription(department.getDescription());

        Department savedDepartment = departmentRepository.save(newDepartment);

        return modelMapper.map(savedDepartment, DepartmentResponseDTO.class);
    }

    public DepartmentResponseDTO updateDepartment(UUID publicId, DepartmentUpdateDTO departmentUpdateDTO)
    {
        Department existingDepartment = departmentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Department.class.getSimpleName(), "publicId", publicId));

        if (departmentRepository.findByName(departmentUpdateDTO.getName()).isPresent() && !existingDepartment.getName().equals(departmentUpdateDTO.getName()))
        {
            throw new ResourceConflictException(String.format(DEPARTMENT_ALREADY_EXISTS, departmentUpdateDTO.getName()), ErrorCode.DATA_CONFLICT,
                    Map.of("field", "name", "value", departmentUpdateDTO.getName()));
        }

        existingDepartment.setName(departmentUpdateDTO.getName());
        existingDepartment.setDescription(departmentUpdateDTO.getDescription());

        Department updatedDepartment = departmentRepository.save(existingDepartment);
        return modelMapper.map(updatedDepartment, DepartmentResponseDTO.class);
    }

    public DepartmentResponseDTO patchDepartment(UUID publicId, DepartmentUpdateDTO departmentUpdateDTO)
    {
        Department existingDepartment = departmentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Department.class.getSimpleName(), "publicId", publicId));

        if (departmentUpdateDTO.getName() != null)
        {
            if (departmentRepository.findByName(departmentUpdateDTO.getName()).isPresent() && !existingDepartment.getName().equals(departmentUpdateDTO.getName()))
            {
                throw new ResourceConflictException(String.format(DEPARTMENT_ALREADY_EXISTS, departmentUpdateDTO.getName()), ErrorCode.DATA_CONFLICT,
                        Map.of("field", "name", "value", departmentUpdateDTO.getName()));
            }
            existingDepartment.setName(departmentUpdateDTO.getName());
        }

        if (departmentUpdateDTO.getDescription() != null)
        {
            existingDepartment.setDescription(departmentUpdateDTO.getDescription());
        }

        Department updatedDepartment = departmentRepository.save(existingDepartment);
        return modelMapper.map(updatedDepartment, DepartmentResponseDTO.class);
    }

    public DepartmentResponseDTO getDepartmentByPublicId(UUID publicId)
    {
        Department department = departmentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(Department.class.getSimpleName(), "publicId", publicId));
        return modelMapper.map(department, DepartmentResponseDTO.class);
    }

    public PageResponse<DepartmentResponseDTO> getAllDepartments(Pageable pageable)
    {
        return PageResponse.from(departmentRepository.findAll(pageable),
                department -> modelMapper.map(department, DepartmentResponseDTO.class));
    }
}
