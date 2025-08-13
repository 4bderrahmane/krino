package com.InterviewManager.interview_slot_manager.DTO.Interview;

import com.InterviewManager.interview_slot_manager.DTO.Job.JobResponseDTO;
import com.InterviewManager.interview_slot_manager.DTO.User.UserResponseDTO;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class InterviewResponseDTO {

    private Long id;
    private UserResponseDTO interviewer;
    private UserResponseDTO candidate;
    private JobResponseDTO job;
    private LocalDate interviewDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isOnline;

}