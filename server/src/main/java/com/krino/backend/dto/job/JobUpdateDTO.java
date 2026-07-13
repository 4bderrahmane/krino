package com.krino.backend.dto.job;

import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.ExperienceLevel;
import com.krino.backend.entity.enums.MoroccanCity;
import com.krino.backend.entity.enums.RemotePolicy;
import com.krino.backend.entity.enums.SalaryCurrency;
import com.krino.backend.entity.enums.SalaryPeriod;
import com.krino.backend.validation.ValidationGroups;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
public class JobUpdateDTO {
    @NotNull(message = "Title cannot be null", groups = ValidationGroups.FullUpdate.class)
    @Pattern(regexp = "(?s).*\\S.*", message = "Title cannot be blank",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    @Size(max = 100, message = "Title cannot exceed 100 characters",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    private String title;

    @Pattern(regexp = "(?s).*\\S.*", message = "Description cannot be blank",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    @Size(max = 4000, message = "Description cannot exceed 4000 characters",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    private String description;

    @PositiveOrZero(message = "Minimum salary cannot be negative",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    private Integer salaryMin;

    @PositiveOrZero(message = "Maximum salary cannot be negative",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    private Integer salaryMax;

    private SalaryCurrency salaryCurrency;

    private SalaryPeriod salaryPeriod;

    private Boolean salaryNegotiable;

    @Future(message = "Application deadline must be in the future",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    private Instant applicationDeadline;

    private LocalDate plannedStartDate;

    private MoroccanCity city;

    @NotNull(message = "Remote policy cannot be null", groups = ValidationGroups.FullUpdate.class)
    private RemotePolicy remotePolicy;

    private ExperienceLevel experienceLevel;

    @PositiveOrZero(message = "Minimum experience years cannot be negative",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    private Integer minimumExperienceYears;

    @Positive(message = "Open positions must be greater than zero",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    private Integer openPositions;

    @NotNull(message = "Department name cannot be null", groups = ValidationGroups.FullUpdate.class)
    @Pattern(regexp = "(?s).*\\S.*", message = "Department name cannot be blank",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    @Size(max = 100, message = "Department name cannot exceed 100 characters",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    private String departmentName;

    @NotNull(message = "Employment type cannot be null", groups = ValidationGroups.FullUpdate.class)
    private EmploymentType employmentType;

    @NotNull(message = "Contract type cannot be null", groups = ValidationGroups.FullUpdate.class)
    private ContractType contractType;

    @Valid
    @Size(max = 30, message = "A job cannot have more than 30 skills",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    private List<JobSkillRequestDTO> skills;

    @AssertTrue(message = "Minimum salary cannot exceed maximum salary",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    public boolean isSalaryRangeValid() {
        return salaryMin == null || salaryMax == null || salaryMin <= salaryMax;
    }
}
