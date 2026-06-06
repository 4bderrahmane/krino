package com.krino.backend.dto.department;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

public class DepartmentResponseDTO
{

    private Long id;
    private String name;
    private String description;
}
