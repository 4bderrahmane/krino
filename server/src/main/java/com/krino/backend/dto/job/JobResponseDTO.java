package com.krino.backend.dto.job;

import com.krino.backend.dto.department.DepartmentResponseDTO;
import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.ExperienceLevel;
import com.krino.backend.entity.enums.JobStatus;
import com.krino.backend.entity.enums.MoroccanCity;
import com.krino.backend.entity.enums.RemotePolicy;
import com.krino.backend.entity.enums.SalaryCurrency;
import com.krino.backend.entity.enums.SalaryPeriod;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class JobResponseDTO {
    private UUID id;
    private String title;
    private String description;
    private DepartmentResponseDTO department;
    private Instant applicationDeadline;
    private Integer salaryMin;
    private Integer salaryMax;
    private SalaryCurrency salaryCurrency;
    private SalaryPeriod salaryPeriod;
    private MoroccanCity city;
    private RemotePolicy remotePolicy;
    private ExperienceLevel experienceLevel;
    private Integer openPositions;
    private EmploymentType employmentType;
    private ContractType contractType;
    private JobStatus status;
    private List<JobSkillResponseDTO> skills;
}
