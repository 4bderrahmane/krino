package com.jesa.interviewslotmanager.dto.slot;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SlotUpdateDTO
{
    private Boolean available;
    private LocalDate interviewDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
