package com.jesa.interviewslotmanager.service;

import com.jesa.interviewslotmanager.dto.department.DepartmentCreateDTO;
import com.jesa.interviewslotmanager.entity.Department;
import com.jesa.interviewslotmanager.exception.DepartmentNotFoundException;
import com.jesa.interviewslotmanager.exception.ResourceConflictException;
import com.jesa.interviewslotmanager.repository.DepartmentRepository;
import com.jesa.interviewslotmanager.utility.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Transactional
public class DepartmentService
{
    private static final String DEPARTMENT_NOT_FOUND = "Department not found";
    private static final String DEPARTMENT_ALREADY_EXISTS = "Department already exists";
    private final DepartmentRepository departmentRepository;

    public void deleteDepartmentById(Long id)
    {
        if (!departmentRepository.existsById(id))
        {
            throw new DepartmentNotFoundException(DEPARTMENT_NOT_FOUND);
        }
        departmentRepository.deleteById(id);
    }

    public Department createDepartment(DepartmentCreateDTO department)
    {
        if (departmentRepository.findByName(department.getName()).isPresent())
        {
            throw new ResourceConflictException(String.format(DEPARTMENT_ALREADY_EXISTS, department.getName()), ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        Department newDepartment = new Department();
        newDepartment.setName(department.getName());
        newDepartment.setDescription(department.getDescription());
        return departmentRepository.save(newDepartment);
    }
}
