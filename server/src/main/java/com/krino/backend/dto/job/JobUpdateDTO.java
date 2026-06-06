package com.krino.backend.dto.job;

import lombok.Data;

@Data
public class JobUpdateDTO
{
    private String title;
    private String description;
    private String departmentName;
    private String type;
    private String status;
}