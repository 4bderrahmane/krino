package com.jesa.interviewslotmanager.dto.interview;

import com.jesa.interviewslotmanager.dto.job.JobResponseDTO;
import com.jesa.interviewslotmanager.dto.user.UserResponseDTO;
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