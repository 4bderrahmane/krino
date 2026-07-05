package com.krino.backend.entity;

import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.ExperienceLevel;
import com.krino.backend.entity.enums.JobStatus;
import com.krino.backend.entity.enums.MoroccanCity;
import com.krino.backend.entity.enums.RemotePolicy;
import com.krino.backend.entity.enums.SalaryCurrency;
import com.krino.backend.entity.enums.SalaryPeriod;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

@Getter
@Entity
@Table(
        name = "job_postings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_job_postings_reference_code", columnNames = "reference_code"),
                @UniqueConstraint(name = "uk_job_postings_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_job_postings_department_status", columnList = "department_id, status"),
                @Index(name = "idx_job_postings_status_published", columnList = "status, published_at"),
                @Index(name = "idx_job_postings_application_deadline", columnList = "application_deadline")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job extends AbstractPublicEntity {
    public static final String TITLE_STRING = "title";

    @NotBlank
    @Size(max = 50)
    @Column(name = "reference_code", nullable = false, updatable = false, length = 50)
    private String referenceCode;

    @NotBlank
    @Size(max = 180)
    @Column(name = "slug", nullable = false, length = 180)
    private String slug;

    @NotNull
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false, foreignKey = @ForeignKey(name = "fk_job_postings_department"))
    private Department department;

    @NotBlank
    @Size(max = 180)
    @Column(nullable = false, length = 180)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "application_deadline")
    private Instant applicationDeadline;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @PositiveOrZero
    @Column(name = "salary_min")
    private Integer salaryMin;

    @PositiveOrZero
    @Column(name = "salary_max")
    private Integer salaryMax;

    @Enumerated(STRING)
    @Column(name = "salary_currency", length = 3)
    private SalaryCurrency salaryCurrency;

    @Enumerated(STRING)
    @Column(name = "salary_period", length = 30)
    private SalaryPeriod salaryPeriod;

    @Column(name = "salary_visible", nullable = false)
    private boolean salaryVisible = false;

    @Column(name = "salary_negotiable", nullable = false)
    private boolean salaryNegotiable = false;

    // Nullable for fully remote work.
    @Enumerated(STRING)
    @Column(length = 50)
    private MoroccanCity city;

    @NotNull
    @Enumerated(STRING)
    @Column(name = "remote_policy", nullable = false, length = 30)
    private RemotePolicy remotePolicy;

    @Enumerated(STRING)
    @Column(name = "experience_level", length = 30)
    private ExperienceLevel experienceLevel;

    @PositiveOrZero
    @Column(name = "minimum_experience_years")
    private Integer minimumExperienceYears;

    @Positive
    @Column(name = "open_positions", nullable = false)
    private int openPositions = 1;

    @NotNull
    @Enumerated(STRING)
    @Column(name = "employment_type", nullable = false, length = 30)
    private EmploymentType employmentType;

    @NotNull
    @Enumerated(STRING)
    @Column(name = "contract_type", nullable = false, length = 30)
    private ContractType contractType;

    @NotNull
    @Enumerated(STRING)
    @Column(nullable = false, length = 30)
    private JobStatus status = JobStatus.DRAFT;

    @Column(name = "scheduled_publish_at")
    private Instant scheduledPublishAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private transient Set<JobSkill> skills = new LinkedHashSet<>();

    public Job(String rCode, String s, Department d, String t, EmploymentType eType, ContractType cType, RemotePolicy rPolicy) {
        referenceCode = requireText(rCode, "referenceCode");
        slug = requireText(s, "slug");
        department = Objects.requireNonNull(d, "department must not be null");
        title = requireText(t, TITLE_STRING);
        employmentType = Objects.requireNonNull(eType, "employmentType must not be null");
        contractType = Objects.requireNonNull(cType, "contractType must not be null");
        remotePolicy = Objects.requireNonNull(rPolicy, "remotePolicy must not be null");
    }

    public void updateContent(String t, String d) {
        ensureEditable();

        title = requireText(t, TITLE_STRING);
        description = normalizeNullable(d);
    }

    public void updateClassification(EmploymentType eType, ContractType cType, ExperienceLevel eLevel, Integer minEYears, int o) {
        ensureEditable();

        employmentType = Objects.requireNonNull(eType, "employmentType must not be null");
        contractType = Objects.requireNonNull(cType, "contractType must not be null");
        experienceLevel = eLevel;
        minimumExperienceYears = minEYears;
        openPositions = o;
    }

    public void updateWorkArrangement(RemotePolicy remotePolicy, MoroccanCity city) {
        ensureEditable();

        Objects.requireNonNull(remotePolicy, "remotePolicy must not be null");
        validateLocation(remotePolicy, city);

        this.remotePolicy = remotePolicy;
        this.city = city;
    }

    public void updateTimeline(Instant aDeadline, LocalDate psDate) {
        ensureEditable();

        if (applicationDeadline != null && publishedAt != null && !applicationDeadline.isAfter(publishedAt))
            throw new IllegalArgumentException("applicationDeadline must be after publishedAt");

        applicationDeadline = aDeadline;
        plannedStartDate = psDate;
    }

    public void updateSalary(Integer sMin, Integer sMax, SalaryCurrency cur, SalaryPeriod sPer, boolean vis, boolean sNeg) {
        ensureEditable();
        validateSalary(sMin, sMax, cur, sPer, vis);

        salaryMin = sMin;
        salaryMax = sMax;
        salaryCurrency = cur;
        salaryPeriod = sPer;
        salaryVisible = vis;
        salaryNegotiable = sNeg;
    }

    public void clearSalary() {
        ensureEditable();
        salaryMin = null;
        salaryMax = null;
        salaryCurrency = null;
        salaryPeriod = null;
        salaryVisible = false;
        salaryNegotiable = false;
    }

    public void changeDepartment(Department d) {
        ensureEditable();
        department = Objects.requireNonNull(d, "department must not be null");
    }

    public void schedulePublication(Instant publishAt) {
        if (status != JobStatus.DRAFT) throw new IllegalStateException("Only a draft posting can be scheduled");

        Objects.requireNonNull(publishAt, "publishAt must not be null");
        validatePublishable(publishAt);

        scheduledPublishAt = publishAt;
        status = JobStatus.SCHEDULED;
    }

    public void publish(Instant pubAt) {
        Objects.requireNonNull(pubAt, "publishedAt must not be null");

        if (status != JobStatus.DRAFT && status != JobStatus.SCHEDULED && status != JobStatus.PAUSED)
            throw new IllegalStateException("Posting cannot be published from status " + status);

        validatePublishable(pubAt);

        if (publishedAt == null) publishedAt = pubAt;

        scheduledPublishAt = null;
        closedAt = null;
        status = JobStatus.OPEN;
    }

    public void pause() {
        if (status != JobStatus.OPEN) throw new IllegalStateException("Only an open posting can be paused");
        status = JobStatus.PAUSED;
    }

    public void close(JobStatus closingStatus, Instant closedAt) {
        Objects.requireNonNull(closingStatus, "closingStatus must not be null");
        Objects.requireNonNull(closedAt, "closedAt must not be null");

        if (closingStatus != JobStatus.CLOSED && closingStatus != JobStatus.FILLED && closingStatus != JobStatus.CANCELLED) {
            throw new IllegalArgumentException("Closing status must be CLOSED, FILLED, or CANCELLED");
        }

        if (status == JobStatus.ARCHIVED) {
            throw new IllegalStateException("An archived posting cannot be closed");
        }

        this.status = closingStatus;
        this.closedAt = closedAt;
        this.scheduledPublishAt = null;
    }

    public void archive(Instant archivedAt) {
        Objects.requireNonNull(archivedAt, "archivedAt must not be null");

        if (status == JobStatus.OPEN)
            throw new IllegalStateException("Close the posting before archiving it");

        status = JobStatus.ARCHIVED;

        if (closedAt == null) this.closedAt = archivedAt;
        scheduledPublishAt = null;
    }

    public Set<JobSkill> getSkills() {
        return Collections.unmodifiableSet(skills);
    }

    public void addSkill(JobSkill jobSkill) {
        Objects.requireNonNull(jobSkill, "jobSkill must not be null");

        if (skills.add(jobSkill)) jobSkill.setJob(this);
    }

    public void removeSkill(JobSkill jobSkill) {
        if (jobSkill == null) return;
        if (skills.remove(jobSkill)) jobSkill.setJob(null);
    }

    public void replaceSkills(Set<JobSkill> newSkills) {
        Set<JobSkill> replacements = newSkills == null ? Set.of() : new LinkedHashSet<>(newSkills);

        skills.forEach(skill -> skill.setJob(null));
        skills.clear();
        replacements.forEach(this::addSkill);
    }

    public boolean isAcceptingApplications(Instant now) {
        Objects.requireNonNull(now, "now must not be null");

        return status == JobStatus.OPEN && (applicationDeadline == null || applicationDeadline.isAfter(now));
    }

    private void ensureEditable() {
        if (status == JobStatus.ARCHIVED)
            throw new IllegalStateException("An archived posting cannot be modified");
    }

    private void validatePublishable(Instant publicationTime) {
        if (isBlank(title)) throw new IllegalStateException("A title is required before publication");
        if (isBlank(description)) throw new IllegalStateException("A description is required before publication");
        if (department == null) throw new IllegalStateException("A department is required before publication");
        if (applicationDeadline != null && !applicationDeadline.isAfter(publicationTime)) throw new IllegalStateException("The application deadline must be after publication");

        validateSalary(salaryMin, salaryMax, salaryCurrency, salaryPeriod, salaryVisible);
    }

    private static void validateSalary(Integer salaryMin, Integer salaryMax, SalaryCurrency currency, SalaryPeriod period, boolean visible) {
        if (salaryMin != null && salaryMax != null && salaryMax < salaryMin) {
            throw new IllegalArgumentException("salaryMax cannot be lower than salaryMin");
        }

        boolean salaryDefined = salaryMin != null || salaryMax != null;

        if (salaryDefined && currency == null) {
            throw new IllegalArgumentException("salaryCurrency is required when salary is defined");
        }

        if (salaryDefined && period == null) {
            throw new IllegalArgumentException("salaryPeriod is required when salary is defined");
        }

        if (!salaryDefined && (currency != null || period != null)) {
            throw new IllegalArgumentException("Salary currency and period require a salary amount");
        }

        if (visible && !salaryDefined) {
            throw new IllegalArgumentException("Salary cannot be visible when no salary is defined");
        }
    }

    private static void validateLocation(RemotePolicy remotePolicy, MoroccanCity city) {
        if ((remotePolicy == RemotePolicy.ON_SITE || remotePolicy == RemotePolicy.HYBRID) && city == null)
            throw new IllegalArgumentException("A city is required for on-site or hybrid postings");
    }

    @PrePersist
    private void beforeInsert() {
        normalizeValues();
        validateInvariants();
    }

    @PreUpdate
    private void beforeUpdate() {
        normalizeValues();
        validateInvariants();
    }

    private void normalizeValues() {
        referenceCode = requireText(referenceCode, "referenceCode");
        slug = requireText(slug, "slug");
        title = requireText(title, TITLE_STRING);

        description = normalizeNullable(description);
    }

    private void validateInvariants() {
        validateSalary(salaryMin, salaryMax, salaryCurrency, salaryPeriod, salaryVisible);
        validateLocation(remotePolicy, city);

        if (publishedAt != null && applicationDeadline != null && !applicationDeadline.isAfter(publishedAt)) {
            throw new IllegalStateException("applicationDeadline must be after publishedAt");
        }

        if ((status == JobStatus.OPEN || status == JobStatus.PAUSED) && publishedAt == null) {
            throw new IllegalStateException("An open or paused posting must have publishedAt");
        }

        if (status == JobStatus.SCHEDULED && scheduledPublishAt == null) {
            throw new IllegalStateException("A scheduled posting must have scheduledPublishAt");
        }

        if (isTerminalStatus(status) && closedAt == null) {
            throw new IllegalStateException("A closed posting must have closedAt");
        }
    }

    private static boolean isTerminalStatus(JobStatus status) {
        return status == JobStatus.CLOSED || status == JobStatus.FILLED || status == JobStatus.CANCELLED || status == JobStatus.ARCHIVED;
    }

    private static String requireText(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) return null;

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Job)) return false;
        Long id = getId();
        return id != null && id.equals(((Job) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
