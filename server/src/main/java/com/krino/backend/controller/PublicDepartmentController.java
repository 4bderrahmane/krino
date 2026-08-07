package com.krino.backend.controller;

import com.krino.backend.dto.department.DepartmentResponseDTO;
import com.krino.backend.service.DepartmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Departments (public)")
@SecurityRequirements
@RestController
@RequestMapping("/api/public/departments")
@RequiredArgsConstructor
public class PublicDepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<List<DepartmentResponseDTO>> getPublicDepartments() {
        return ResponseEntity.ok(departmentService.getPublicDepartments());
    }
}
