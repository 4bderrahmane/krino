package com.jesa.interviewslotmanager.dto.job;

import com.jesa.interviewslotmanager.dto.department.DepartmentResponseDTO;
import lombok.Data;

@Data
public class JobResponseDTO
{
    private Long id;
    private String title;
    private String description;
    private DepartmentResponseDTO department;
}
