package com.krino.backend.dto.slot;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A booked slot reduced to when it happens.
 *
 * <p>{@link SlotResponseDTO} embeds the interviewer as a full {@code UserResponseDTO}, so handing
 * one to a candidate would leak the interviewer's contact details and account state through the
 * nested object even if the enclosing response were careful. This type drops the interviewer
 * entirely; who is conducting the interview is named separately, and only as a summary.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SlotSummaryDTO {

    private UUID id;
    private Integer durationInMinutes;
    // Always serialized in UTC (…Z); the client renders it in whichever zone it displays.
    private Instant startAt;
    private Instant endAt;
}
