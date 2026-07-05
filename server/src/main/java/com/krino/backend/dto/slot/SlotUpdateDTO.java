package com.krino.backend.dto.slot;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SlotUpdateDTO {
    private LocalDate interviewDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // Same forward-window rule as create; null parts are ignored so partial PATCHes still pass.
    @AssertTrue(message = "End time must be after start time")
    public boolean isEndAfterStart() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
