package com.krino.backend.dto.slot;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SlotUpdateDTO {
    private LocalDate interviewDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
