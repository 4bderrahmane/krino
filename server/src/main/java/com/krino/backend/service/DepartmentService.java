package com.krino.backend.service;

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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Transactional
public class DepartmentService
{
    private static final String DEPARTMENT_ALREADY_EXISTS = "Department '%s' already exists";
    private final DepartmentRepository departmentRepository;
    private final ModelMapper modelMapper;

    public void deleteDepartmentById(Long id)
    {
        if (!departmentRepository.existsById(id))
        {
            // use object-name constructor to get DEPARTMENT_NOT_FOUND ErrorCode
            throw new ResourceNotFoundException(Department.class.getSimpleName(), "id", id);
        }
        departmentRepository.deleteById(id);
    }

    public DepartmentResponseDTO createDepartment(DepartmentCreateDTO department)
    {
        if (departmentRepository.findByName(department.getName()).isPresent())
        {
            throw new ResourceConflictException(String.format(DEPARTMENT_ALREADY_EXISTS, department.getName()), ErrorCode.DEPARTMENT_ALREADY_EXISTS);
        }
        Department newDepartment = new Department();

        newDepartment.setName(department.getName());
        newDepartment.setDescription(department.getDescription());

        Department savedDepartment = departmentRepository.save(newDepartment);

        return modelMapper.map(savedDepartment, DepartmentResponseDTO.class);
    }

    public DepartmentResponseDTO updateDepartment(Long id, DepartmentUpdateDTO departmentUpdateDTO)
    {
        Department existingDepartment = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Department.class.getSimpleName(), "id", id));

        if (departmentRepository.findByName(departmentUpdateDTO.getName()).isPresent() && !existingDepartment.getName().equals(departmentUpdateDTO.getName()))
        {
            throw new ResourceConflictException(String.format(DEPARTMENT_ALREADY_EXISTS, departmentUpdateDTO.getName()), ErrorCode.DEPARTMENT_ALREADY_EXISTS);
        }

        existingDepartment.setName(departmentUpdateDTO.getName());
        existingDepartment.setDescription(departmentUpdateDTO.getDescription());

        Department updatedDepartment = departmentRepository.save(existingDepartment);
        return modelMapper.map(updatedDepartment, DepartmentResponseDTO.class);
    }

    public DepartmentResponseDTO patchDepartment(Long id, DepartmentUpdateDTO departmentUpdateDTO)
    {
        Department existingDepartment = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Department.class.getSimpleName(), "id", id));

        if (departmentUpdateDTO.getName() != null)
        {
            if (departmentRepository.findByName(departmentUpdateDTO.getName()).isPresent() && !existingDepartment.getName().equals(departmentUpdateDTO.getName()))
            {
                throw new ResourceConflictException(String.format(DEPARTMENT_ALREADY_EXISTS, departmentUpdateDTO.getName()), ErrorCode.DEPARTMENT_ALREADY_EXISTS);
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

    public DepartmentResponseDTO getDepartmentById(Long id)
    {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Department.class.getSimpleName(), "id", id));
        return modelMapper.map(department, DepartmentResponseDTO.class);
    }

    public List<DepartmentResponseDTO> getAllDepartments()
    {
        return departmentRepository.findAll().stream()
                .map(department -> modelMapper.map(department, DepartmentResponseDTO.class))
                .toList();
    }
}
