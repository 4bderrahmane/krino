package com.jesa.interviewslotmanager.controller;

import com.jesa.interviewslotmanager.dto.department.DepartmentCreateDTO;
import com.jesa.interviewslotmanager.entity.Department;
import com.jesa.interviewslotmanager.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/departments")
public class DepartmentController
{
    private final DepartmentService departmentService;

    @PostMapping("/create")
    public ResponseEntity<Department> createDepartment(@Valid @RequestBody DepartmentCreateDTO request)
    {
        Department department = departmentService.createDepartment(request);
        return ResponseEntity.ok(department);
    }
}
