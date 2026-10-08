package com.krino.backend.dto.slot;

import java.time.Instant;
import java.util.UUID;

import com.krino.backend.dto.user.UserResponseDTO;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlotResponseDTO {

    private UUID id;
    private UserResponseDTO interviewer;
    private Integer durationInMinutes;
    private boolean available = true;
    // Always serialized in UTC (…Z); the client renders it in whichever zone it displays.
    private Instant startAt;
    private Instant endAt;
}
