package com.krino.backend.dto.department;

import lombok.*;

import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

public class DepartmentResponseDTO
{

    private UUID id;
    private String name;
    private String description;
}
