package com.krino.backend.controller;

import com.krino.backend.dto.department.DepartmentCreateDTO;
import com.krino.backend.dto.department.DepartmentResponseDTO;
import com.krino.backend.dto.department.DepartmentUpdateDTO;
import com.krino.backend.service.DepartmentService;
import com.krino.backend.validation.ValidationGroups;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Tag(name = "Departments")
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('department:create')")
    public ResponseEntity<DepartmentResponseDTO> createDepartment(@Valid @RequestBody DepartmentCreateDTO request) {
        DepartmentResponseDTO department = departmentService.createDepartment(request);
        return ResponseEntity.created(URI.create("/api/departments/" + department.getId())).body(department);
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('department:delete')")
    public ResponseEntity<Void> deleteDepartmentByPublicId(@PathVariable UUID publicId) {
        departmentService.deleteDepartmentByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('department:update')")
    public ResponseEntity<DepartmentResponseDTO> editDepartmentCompletelyByPublicId(
            @PathVariable UUID publicId,
            @Validated(ValidationGroups.FullUpdate.class)
            @RequestBody DepartmentUpdateDTO request) {
        DepartmentResponseDTO department = departmentService.updateDepartment(publicId, request);
        return ResponseEntity.ok(department);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('department:update')")
    public ResponseEntity<DepartmentResponseDTO> editDepartmentPartiallyByPublicId(@PathVariable UUID publicId,
                                                                                   @Valid @RequestBody DepartmentUpdateDTO request) {
        DepartmentResponseDTO department = departmentService.patchDepartment(publicId, request);
        return ResponseEntity.ok(department);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('department:read')")
    public ResponseEntity<DepartmentResponseDTO> getDepartmentByPublicId(@PathVariable UUID publicId) {
        DepartmentResponseDTO department = departmentService.getDepartmentByPublicId(publicId);
        return ResponseEntity.ok(department);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'INTERVIEWER')")
    public ResponseEntity<List<DepartmentResponseDTO>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }
}
