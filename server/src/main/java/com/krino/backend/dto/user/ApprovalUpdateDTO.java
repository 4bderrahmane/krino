package com.krino.backend.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalUpdateDTO {

    @NotNull(message = "Approved flag cannot be null")
    private Boolean approved;
}
