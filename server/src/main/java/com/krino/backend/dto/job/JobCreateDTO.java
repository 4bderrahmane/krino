package com.krino.backend.dto.job;

import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.ExperienceLevel;
import com.krino.backend.entity.enums.MoroccanCity;
import com.krino.backend.entity.enums.RemotePolicy;
import com.krino.backend.entity.enums.SalaryCurrency;
import com.krino.backend.entity.enums.SalaryPeriod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
public class JobCreateDTO {
    @NotBlank(message = "Department name cannot be blank")
    @Size(max = 100, message = "Department name cannot exceed 100 characters")
    private String departmentName;

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    @Pattern(regexp = "(?s).*\\S.*", message = "Description cannot be blank")
    @Size(max = 4000, message = "Description cannot exceed 4000 characters")
    private String description;

    @NotNull(message = "Application deadline cannot be null")
    @Future(message = "Application deadline must be in the future")
    private Instant applicationDeadline;

    private LocalDate plannedStartDate;

    @PositiveOrZero(message = "Minimum salary cannot be negative")
    private Integer salaryMin;

    @PositiveOrZero(message = "Maximum salary cannot be negative")
    private Integer salaryMax;

    private SalaryCurrency salaryCurrency;

    private SalaryPeriod salaryPeriod;

    private Boolean salaryVisible = true;

    private Boolean salaryNegotiable = false;

    private MoroccanCity city;

    @NotNull(message = "Remote policy cannot be null")
    private RemotePolicy remotePolicy;

    private ExperienceLevel experienceLevel;

    @PositiveOrZero(message = "Minimum experience years cannot be negative")
    private Integer minimumExperienceYears;

    @Positive(message = "Open positions must be greater than zero")
    private Integer openPositions = 1;

    @NotNull(message = "Employment type cannot be null")
    private EmploymentType employmentType;

    @NotNull(message = "Contract type cannot be null")
    private ContractType contractType;

    @Valid
    @Size(max = 30, message = "A job cannot have more than 30 skills")
    private List<JobSkillRequestDTO> skills = List.of();

    @AssertTrue(message = "Minimum salary cannot exceed maximum salary")
    public boolean isSalaryRangeValid() {
        return salaryMin == null || salaryMax == null || salaryMin <= salaryMax;
    }
}
