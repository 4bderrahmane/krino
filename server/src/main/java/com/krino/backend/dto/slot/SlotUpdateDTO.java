package com.krino.backend.dto.slot;

import com.krino.backend.validation.ValidationGroups;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class SlotUpdateDTO {

    @NotNull(message = "Start time cannot be null", groups = ValidationGroups.FullUpdate.class)
    @Future(message = "A slot must start in the future")
    private Instant startAt;

    @NotNull(message = "End time cannot be null", groups = ValidationGroups.FullUpdate.class)
    @Future(message = "A slot must end in the future")
    private Instant endAt;

    @AssertTrue(message = "End time must be after start time")
    public boolean isEndAfterStart() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }
}
