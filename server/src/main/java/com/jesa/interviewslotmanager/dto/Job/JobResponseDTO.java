package com.jesa.interviewslotmanager.dto.Job;

import com.jesa.interviewslotmanager.dto.Department.DepartmentResponseDTO;
import com.jesa.interviewslotmanager.dto.Slot.SlotResponseDTO;

import java.util.List;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class JobResponseDTO
{

    private Long id;
    private String title;
    private String description;
    private DepartmentResponseDTO department; // Nested DTO
    private List<SlotResponseDTO> slots;
}
