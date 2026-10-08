package com.krino.backend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Just enough of a user to name them on screen.
 * Used wherever one person appears inside another person's record.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {

    private UUID id;
    private String firstName;
    private String lastName;
}
