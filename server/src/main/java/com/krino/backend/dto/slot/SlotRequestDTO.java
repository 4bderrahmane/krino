package com.krino.backend.dto.slot;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class SlotRequestDTO {
    private UUID interviewerId;

    @NotNull(message = "Start time cannot be null")
    @Future(message = "A slot must start in the future")
    private Instant startAt;

    @NotNull(message = "End time cannot be null")
    @Future(message = "A slot must end in the future")
    private Instant endAt;

    @AssertTrue(message = "End time must be after start time")
    @JsonIgnore
    public boolean isEndAfterStart() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }
}
