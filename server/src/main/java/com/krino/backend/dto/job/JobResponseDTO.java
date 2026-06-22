package com.krino.backend.dto.job;

import com.krino.backend.dto.department.DepartmentResponseDTO;
import lombok.Data;

import java.util.UUID;

@Data
public class JobResponseDTO {
    private UUID id;
    private String title;
    private String description;
    private DepartmentResponseDTO department;
}
