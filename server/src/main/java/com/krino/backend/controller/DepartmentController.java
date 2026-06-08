package com.krino.backend.controller;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.department.DepartmentCreateDTO;
import com.krino.backend.dto.department.DepartmentResponseDTO;
import com.krino.backend.dto.department.DepartmentUpdateDTO;
import com.krino.backend.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/departments")
public class DepartmentController
{
    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_DEPARTMENT')")
    public ResponseEntity<DepartmentResponseDTO> createDepartment(@Valid @RequestBody DepartmentCreateDTO request)
    {
        DepartmentResponseDTO department = departmentService.createDepartment(request);
        return ResponseEntity.created(URI.create("/api/departments/" + department.getId())).body(department);
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_DELETE_DEPARTMENT')")
    public ResponseEntity<Void> deleteDepartmentByPublicId(@PathVariable("publicId") UUID publicId)
    {
        departmentService.deleteDepartmentByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_DEPARTMENT')")
    public ResponseEntity<DepartmentResponseDTO> editDepartmentCompletelyByPublicId(@PathVariable("publicId") UUID publicId, @Valid @RequestBody DepartmentUpdateDTO request)
    {
        DepartmentResponseDTO department = departmentService.updateDepartment(publicId, request);
        return ResponseEntity.ok(department);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_DEPARTMENT')")
    public ResponseEntity<DepartmentResponseDTO> editDepartmentPartiallyByPublicId(@PathVariable("publicId") UUID publicId, @Valid @RequestBody DepartmentUpdateDTO request)
    {
        DepartmentResponseDTO department = departmentService.patchDepartment(publicId, request);
        return ResponseEntity.ok(department);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('CAN_READ_DEPARTMENT')")
    public ResponseEntity<DepartmentResponseDTO> getDepartmentByPublicId(@PathVariable("publicId") UUID publicId)
    {
        DepartmentResponseDTO department = departmentService.getDepartmentByPublicId(publicId);
        return ResponseEntity.ok(department);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_READ_DEPARTMENT')")
    public ResponseEntity<PageResponse<DepartmentResponseDTO>> getAllDepartments(@PageableDefault(size = 20, sort = "id") Pageable pageable)
    {
        PageResponse<DepartmentResponseDTO> departments = departmentService.getAllDepartments(pageable);
        return ResponseEntity.ok(departments);
    }
}
