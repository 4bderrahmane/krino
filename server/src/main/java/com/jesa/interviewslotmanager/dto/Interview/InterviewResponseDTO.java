package com.jesa.interviewslotmanager.dto.Interview;

import com.jesa.interviewslotmanager.dto.Job.JobResponseDTO;
import com.jesa.interviewslotmanager.dto.User.UserResponseDTO;
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