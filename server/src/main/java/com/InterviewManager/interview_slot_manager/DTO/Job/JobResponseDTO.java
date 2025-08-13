package com.InterviewManager.interview_slot_manager.DTO.Job;

import com.InterviewManager.interview_slot_manager.DTO.Department.DepartmentResponseDTO;
import com.InterviewManager.interview_slot_manager.DTO.Slot.SlotResponseDTO;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class JobResponseDTO {

    private Long id;
    private String title;
    private String description;
    private DepartmentResponseDTO department; // Nested DTO
    private List<SlotResponseDTO> slots;
}
