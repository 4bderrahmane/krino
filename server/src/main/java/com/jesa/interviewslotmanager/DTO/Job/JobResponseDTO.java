package com.jesa.interviewslotmanager.DTO.Job;

import com.jesa.interviewslotmanager.DTO.Department.DepartmentResponseDTO;
import com.jesa.interviewslotmanager.DTO.Slot.SlotResponseDTO;

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
