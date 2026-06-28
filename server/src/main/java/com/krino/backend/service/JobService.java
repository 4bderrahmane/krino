package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.job.JobCreateDTO;
import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.dto.job.JobSkillRequestDTO;
import com.krino.backend.dto.job.JobUpdateDTO;
import com.krino.backend.entity.enums.SalaryCurrency;
import com.krino.backend.entity.enums.SalaryPeriod;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.JobSkill;
import com.krino.backend.entity.Skill;
import com.krino.backend.entity.enums.JobStatus;
import com.krino.backend.entity.enums.SkillImportance;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.JobMapper;
import com.krino.backend.repository.ApplicationRepository;
import com.krino.backend.repository.DepartmentRepository;
import com.krino.backend.repository.JobRepository;
import com.krino.backend.repository.SkillRepository;
import com.krino.backend.utility.ErrorCode;
import com.krino.backend.utility.Slugs;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@Transactional
@Service
@RequiredArgsConstructor
public class JobService {
    private static final String JOB_NOT_FOUND_MESSAGE = "Job with public ID '%s' not found.";
    private static final String DEPARTMENT_NOT_FOUND_MESSAGE = "Department with name '%s' not found.";
    private final JobRepository jobRepository;
    private final DepartmentRepository departmentRepository;
    private final ApplicationRepository applicationRepository;
    private final SkillRepository skillRepository;
    private final JobMapper jobMapper;

    public void deleteJobByPublicId(UUID publicId) {
        Job job = findJob(publicId);

        if (applicationRepository.existsByJob(job)) {
            throw new ResourceConflictException(
                    String.format("Job '%s' has applications or interviews and cannot be deleted; close it instead.",
                            job.getTitle()),
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of("resource", "Job", "title", job.getTitle()));
        }

        jobRepository.delete(job);
    }

    public JobResponseDTO createJob(JobCreateDTO dto) {
        Department department = findDepartment(dto.getDepartmentName());

        Job job = new Job(generateReferenceCode(), generateSlug(dto.getTitle()), department,
                dto.getTitle(), dto.getEmploymentType(), dto.getContractType(), dto.getRemotePolicy());

        try {
            job.updateContent(dto.getTitle(), dto.getDescription());
            job.updateClassification(dto.getEmploymentType(), dto.getContractType(),
                    dto.getExperienceLevel(), dto.getMinimumExperienceYears(),
                    openPositionsOrDefault(dto.getOpenPositions()));
            job.updateWorkArrangement(dto.getRemotePolicy(), dto.getCity());
            job.updateTimeline(dto.getApplicationDeadline(), dto.getPlannedStartDate());
            applySalary(job, dto.getSalaryMin(), dto.getSalaryMax(), dto.getSalaryCurrency(),
                    dto.getSalaryPeriod(), dto.getSalaryVisible(), dto.getSalaryNegotiable());
            job.replaceSkills(resolveJobSkills(dto.getSkills()));
            return jobMapper.toResponse(jobRepository.save(job));
        } catch (IllegalStateException ex) {
            throw notAllowed(ex);
        }
    }

    public JobResponseDTO getJobByPublicId(UUID publicId) {
        return jobMapper.toResponse(findJob(publicId));
    }

    // Offers are intentionally NOT paginated. The offer catalogue is small and
    // bounded by design — we keep only the offers we currently need and delete
    // stale ones, so it never grows large enough to justify server-side paging.
    // We therefore ignore the incoming Pageable and return the WHOLE list in a
    // single page; the web client fetches everything and does its own filtering
    // (e.g. by department). Larger collections (applications, interviews, ...)
    // DO paginate on the server, so they keep a real Pageable.
    public PageResponse<JobResponseDTO> getAllJobs(Pageable pageable) {
        return PageResponse.from(jobRepository.findAll(Pageable.unpaged()),
                jobMapper::toResponse);
    }

    // PUT — full replace. The FullUpdate validation group guarantees the core
    // fields are present; every behaviour-method group is applied unconditionally.
    public JobResponseDTO updateJob(UUID publicId, JobUpdateDTO dto) {
        return mutate(publicId, job -> applyFullUpdate(job, dto));
    }

    // PATCH — partial. A group is touched only when at least one of its fields is
    // present; unspecified fields in a touched group keep the entity's current value
    // (a plain DTO can't tell "absent" from "explicit null", so null means "unchanged").
    public JobResponseDTO patchJob(UUID publicId, JobUpdateDTO dto) {
        return mutate(publicId, job -> applyPatch(job, dto));
    }

    public JobResponseDTO publishJob(UUID publicId) {
        return mutate(publicId, job -> job.publish(Instant.now()));
    }

    public JobResponseDTO pauseJob(UUID publicId) {
        return mutate(publicId, Job::pause);
    }

    public JobResponseDTO closeJob(UUID publicId, JobStatus closingStatus) {
        return mutate(publicId, job -> job.close(closingStatus, Instant.now()));
    }

    public JobResponseDTO archiveJob(UUID publicId) {
        return mutate(publicId, job -> job.archive(Instant.now()));
    }

    // Loads the aggregate, runs a guarded mutation, persists and renders it.
    // Lifecycle/edit guards throw IllegalStateException (invalid transition, archived,
    // ...) — surface those as 409 rather than letting them become a 500.
    private JobResponseDTO mutate(UUID publicId, Consumer<Job> action) {
        Job job = findJob(publicId);
        try {
            action.accept(job);
            return jobMapper.toResponse(jobRepository.save(job));
        } catch (IllegalStateException ex) {
            throw notAllowed(ex);
        }
    }

    private void applyFullUpdate(Job job, JobUpdateDTO dto) {
        if (dto.getDepartmentName() != null) {
            job.changeDepartment(findDepartment(dto.getDepartmentName()));
        }
        job.updateContent(dto.getTitle(), dto.getDescription());
        job.updateClassification(dto.getEmploymentType(), dto.getContractType(),
                dto.getExperienceLevel(), dto.getMinimumExperienceYears(),
                openPositionsOrDefault(dto.getOpenPositions()));
        job.updateWorkArrangement(dto.getRemotePolicy(), dto.getCity());
        job.updateTimeline(dto.getApplicationDeadline(), dto.getPlannedStartDate());
        applySalary(job, dto.getSalaryMin(), dto.getSalaryMax(), dto.getSalaryCurrency(),
                dto.getSalaryPeriod(), dto.getSalaryVisible(), dto.getSalaryNegotiable());
        replaceSkillsIfPresent(job, dto.getSkills());
    }

    private void applyPatch(Job job, JobUpdateDTO dto) {
        if (dto.getDepartmentName() != null) {
            job.changeDepartment(findDepartment(dto.getDepartmentName()));
        }

        if (dto.getTitle() != null || dto.getDescription() != null) {
            job.updateContent(
                    orElse(dto.getTitle(), job.getTitle()),
                    orElse(dto.getDescription(), job.getDescription()));
        }

        if (dto.getEmploymentType() != null || dto.getContractType() != null || dto.getExperienceLevel() != null
                || dto.getMinimumExperienceYears() != null || dto.getOpenPositions() != null) {
            job.updateClassification(
                    orElse(dto.getEmploymentType(), job.getEmploymentType()),
                    orElse(dto.getContractType(), job.getContractType()),
                    orElse(dto.getExperienceLevel(), job.getExperienceLevel()),
                    orElse(dto.getMinimumExperienceYears(), job.getMinimumExperienceYears()),
                    orElse(dto.getOpenPositions(), job.getOpenPositions()));
        }

        if (dto.getRemotePolicy() != null || dto.getCity() != null) {
            job.updateWorkArrangement(
                    orElse(dto.getRemotePolicy(), job.getRemotePolicy()),
                    orElse(dto.getCity(), job.getCity()));
        }

        if (dto.getApplicationDeadline() != null || dto.getPlannedStartDate() != null) {
            job.updateTimeline(
                    orElse(dto.getApplicationDeadline(), job.getApplicationDeadline()),
                    orElse(dto.getPlannedStartDate(), job.getPlannedStartDate()));
        }

        if (dto.getSalaryMin() != null || dto.getSalaryMax() != null || dto.getSalaryCurrency() != null
                || dto.getSalaryPeriod() != null || dto.getSalaryVisible() != null
                || dto.getSalaryNegotiable() != null) {
            job.updateSalary(
                    orElse(dto.getSalaryMin(), job.getSalaryMin()),
                    orElse(dto.getSalaryMax(), job.getSalaryMax()),
                    orElse(dto.getSalaryCurrency(), job.getSalaryCurrency()),
                    orElse(dto.getSalaryPeriod(), job.getSalaryPeriod()),
                    orElse(dto.getSalaryVisible(), job.isSalaryVisible()),
                    orElse(dto.getSalaryNegotiable(), job.isSalaryNegotiable()));
        }

        replaceSkillsIfPresent(job, dto.getSkills());
    }

    // No salary amount means no salary at all; clearSalary keeps currency/period/
    // visibility consistent and avoids validateSalary rejecting a stray salaryVisible.
    private void applySalary(Job job, Integer salaryMin, Integer salaryMax, SalaryCurrency currency,
                             SalaryPeriod period, Boolean visible, Boolean negotiable) {
        if (salaryMin == null && salaryMax == null) {
            job.clearSalary();
            return;
        }
        job.updateSalary(salaryMin, salaryMax, currency, period,
                Boolean.TRUE.equals(visible), Boolean.TRUE.equals(negotiable));
    }

    private static <T> T orElse(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private static int openPositionsOrDefault(Integer openPositions) {
        return openPositions != null ? openPositions : 1;
    }

    private Job findJob(UUID publicId) {
        return jobRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(JOB_NOT_FOUND_MESSAGE, publicId)));
    }

    private Department findDepartment(String name) {
        return departmentRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(DEPARTMENT_NOT_FOUND_MESSAGE, name)));
    }

    private ResourceConflictException notAllowed(IllegalStateException ex) {
        return new ResourceConflictException(ex.getMessage(), ErrorCode.OPERATION_NOT_ALLOWED, Map.of());
    }

    private String generateReferenceCode() {
        return "JOB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String generateSlug(String title) {
        return Slugs.slugify(title) + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    private void replaceSkillsIfPresent(Job job, List<JobSkillRequestDTO> skills) {
        if (skills != null) {
            job.replaceSkills(resolveJobSkills(skills));
        }
    }

    private Set<JobSkill> resolveJobSkills(List<JobSkillRequestDTO> skillRequests) {
        Set<JobSkill> resolved = new LinkedHashSet<>();
        if (skillRequests == null || skillRequests.isEmpty()) {
            return resolved;
        }

        Set<String> seenSlugs = new LinkedHashSet<>();
        for (JobSkillRequestDTO request : skillRequests) {
            if (request == null) {
                throw new IllegalArgumentException("Skill entry cannot be null.");
            }

            String name = normalizeSkillName(request.getName());
            String slug = Slugs.slugify(name);
            if (!seenSlugs.add(slug)) {
                throw new ResourceConflictException(
                        "Duplicate skill '%s' in job request.".formatted(name),
                        ErrorCode.DATA_CONFLICT,
                        Map.of("skill", name));
            }

            Skill skill = skillRepository.findBySlug(slug)
                    .orElseGet(() -> {
                        Skill created = new Skill();
                        created.setName(name);
                        created.setSlug(slug);
                        return skillRepository.save(created);
                    });

            JobSkill jobSkill = new JobSkill();
            jobSkill.setSkill(skill);
            jobSkill.setImportance(request.getImportance() != null
                    ? request.getImportance()
                    : SkillImportance.REQUIRED);
            resolved.add(jobSkill);
        }
        return resolved;
    }

    private static String normalizeSkillName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

}
