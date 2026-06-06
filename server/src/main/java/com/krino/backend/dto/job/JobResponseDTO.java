package com.krino.backend.dto.job;

import com.krino.backend.dto.department.DepartmentResponseDTO;
import lombok.Data;

@Data
public class JobResponseDTO
{
    private Long id;
    private String title;
    private String description;
    private DepartmentResponseDTO department;
}
