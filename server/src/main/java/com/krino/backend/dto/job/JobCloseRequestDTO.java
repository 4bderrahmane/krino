package com.krino.backend.dto.job;

import com.krino.backend.entity.enums.JobStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobCloseRequestDTO {

    /**
     * Why the posting is being closed. Must be one of CLOSED, FILLED or CANCELLED;
     * the value is validated by {@link com.krino.backend.entity.Job#close}.
     */
    @NotNull(message = "Closing status cannot be null")
    private JobStatus status;
}
