package com.krino.backend.controller;

import com.krino.backend.dto.department.DepartmentCreateDTO;
import com.krino.backend.dto.department.DepartmentResponseDTO;
import com.krino.backend.dto.department.DepartmentUpdateDTO;
import com.krino.backend.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/departments")
public class DepartmentController
{
    private final DepartmentService departmentService;

    @PostMapping("/create")
    public ResponseEntity<DepartmentResponseDTO> createDepartment(@Valid @RequestBody DepartmentCreateDTO request)
    {
        DepartmentResponseDTO department = departmentService.createDepartment(request);
        return ResponseEntity.ok(department);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDepartmentById(@PathVariable("id") Long id)
    {
        departmentService.deleteDepartmentById(id);
        return ResponseEntity.ok("Department deleted");
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponseDTO> editDepartmentCompletelyById(@PathVariable("id") Long id, @Valid @RequestBody DepartmentUpdateDTO request)
    {
        DepartmentResponseDTO department = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(department);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DepartmentResponseDTO> editDepartmentPartiallyById(@PathVariable("id") Long id, @Valid @RequestBody DepartmentUpdateDTO request)
    {
        DepartmentResponseDTO department = departmentService.patchDepartment(id, request);
        return ResponseEntity.ok(department);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponseDTO> getDepartmentById(@PathVariable("id") Long id)
    {
        DepartmentResponseDTO department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(department);
    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponseDTO>> getAllDepartments()
    {
        List<DepartmentResponseDTO> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(departments);
    }
}
