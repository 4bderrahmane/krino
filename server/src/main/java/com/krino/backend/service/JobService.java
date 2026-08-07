package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.job.JobCreateDTO;
import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.dto.job.JobSkillRequestDTO;
import com.krino.backend.dto.job.JobUpdateDTO;
import com.krino.backend.entity.CustomUserDetails;
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
import com.krino.backend.utility.SecurityUtilities;
import com.krino.backend.utility.Slugs;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static com.krino.backend.configuration.CachingConfiguration.JOBS_CACHE;
import static com.krino.backend.configuration.CachingConfiguration.JOB_LISTINGS_CACHE;

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
    private static final String ADMIN = "ADMIN";
    private static final String HR_MANAGER = "HR_MANAGER";
    private static final String INTERVIEWER = "INTERVIEWER";

    private final JobRepository jobRepository;
    private final DepartmentRepository departmentRepository;
    private final ApplicationRepository applicationRepository;
    private final SkillRepository skillRepository;
    private final JobMapper jobMapper;
    private final JobReader jobReader;

    @Caching(evict = {
            @CacheEvict(cacheNames = JOBS_CACHE, key = "#publicId"),
            @CacheEvict(cacheNames = JOB_LISTINGS_CACHE, allEntries = true)
    })
    public void deleteJobByPublicId(UUID publicId) {
        Job job = findJob(publicId);

        if (applicationRepository.existsByJob(job)) {
            throw new ResourceConflictException(
                    String.format("Job '%s' has applications or interviews and cannot be deleted; close it instead.", job.getTitle()),
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of("resource", "Job", "title", job.getTitle()));
        }

        jobRepository.delete(job);
    }

    @CacheEvict(cacheNames = JOB_LISTINGS_CACHE, allEntries = true)
    public JobResponseDTO createJob(JobCreateDTO dto) {
        Department department = findDepartment(dto.getDepartmentName());

        Job job = new Job(
                generateReferenceCode(),
                generateSlug(dto.getTitle()),
                department,
                dto.getTitle(),
                dto.getEmploymentType(),
                dto.getContractType(),
                dto.getRemotePolicy()
        );

        try {
            job.updateContent(dto.getTitle(), dto.getDescription());
            job.updateClassification(dto.getEmploymentType(), dto.getContractType(),
                                     dto.getExperienceLevel(), dto.getMinimumExperienceYears(),
                                     openPositionsOrDefault(dto.getOpenPositions()));
            job.updateWorkArrangement(dto.getRemotePolicy(), dto.getCity());
            job.updateTimeline(dto.getApplicationDeadline(), dto.getPlannedStartDate());

            applySalary(job, dto.getSalaryMin(), dto.getSalaryMax(), dto.getSalaryCurrency(), dto.getSalaryPeriod(), dto.getSalaryNegotiable());
            job.replaceSkills(resolveJobSkills(dto.getSkills()));
            return jobMapper.toResponse(jobRepository.save(job));
        } catch (IllegalStateException ex) {
            throw notAllowed(ex);
        }
    }

    /**
     * The authenticated detail view. Staff see any posting; everyone else sees a posting only
     * while it is {@link JobStatus#OPEN}, or afterwards if they applied to it, so that a
     * candidate's own application list never points at a 404.
     */
    public JobResponseDTO getJobByPublicId(UUID publicId) {
        JobResponseDTO job = jobReader.readByPublicId(publicId);
        requireVisible(job);
        return job;
    }

    /**
     * The anonymous detail view: published postings only, and nothing else exists.
     */
    public JobResponseDTO getPublicJobByPublicId(UUID publicId) {
        JobResponseDTO job = jobReader.readByPublicId(publicId);
        if (job.getStatus() != JobStatus.OPEN) {
            throw notFound(publicId);
        }
        return job;
    }

    /**
     * The anonymous catalogue: published postings only, paginated in the database.
     */
    public PageResponse<JobResponseDTO> getOpenJobs(Pageable pageable) {
        return jobReader.readOpenListing(pageable);
    }

    /**
     * The staff catalogue: every posting in every status, drafts included.
     *
     * <p>Deliberately uncached. Its results are staff-visible only, so a cache entry here is one
     * lookup mistake away from serving a draft to a candidate, and the page/sort/filter spread
     * makes the hit rate poor anyway.
     */
    public PageResponse<JobResponseDTO> getAllJobs(Pageable pageable) {
        SecurityUtilities.requireAnyRole(ADMIN, HR_MANAGER, INTERVIEWER);
        return PageResponse.from(jobRepository.findAll(pageable), jobMapper::toResponse);
    }

    // Authorization deliberately runs on the rendered DTO rather than inside the cached read:
    // see JobReader. 404 rather than 403, because 403 on an unpublished posting confirms that
    // it exists, which is exactly what a draft should not leak.
    private void requireVisible(JobResponseDTO job) {
        if (SecurityUtilities.hasAnyRole(ADMIN, HR_MANAGER, INTERVIEWER)
                || job.getStatus() == JobStatus.OPEN) {
            return;
        }

        CustomUserDetails currentUser = SecurityUtilities.requireCurrentCustomUser();
        if (!applicationRepository.existsByJob_PublicIdAndCandidate_PublicId(job.getId(), currentUser.getPublicId())) {
            throw notFound(job.getId());
        }
    }

    private ResourceNotFoundException notFound(UUID publicId) {
        return new ResourceNotFoundException(String.format("Job with public ID '%s' not found.", publicId));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = JOBS_CACHE, key = "#publicId"),
            @CacheEvict(cacheNames = JOB_LISTINGS_CACHE, allEntries = true)
    })
    public JobResponseDTO updateJob(UUID publicId, JobUpdateDTO dto) {
        return mutate(publicId, job -> applyFullUpdate(job, dto));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = JOBS_CACHE, key = "#publicId"),
            @CacheEvict(cacheNames = JOB_LISTINGS_CACHE, allEntries = true)
    })
    public JobResponseDTO patchJob(UUID publicId, JobUpdateDTO dto) {
        return mutate(publicId, job -> applyPatch(job, dto));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = JOBS_CACHE, key = "#publicId"),
            @CacheEvict(cacheNames = JOB_LISTINGS_CACHE, allEntries = true)
    })
    public JobResponseDTO publishJob(UUID publicId) {
        return mutate(publicId, job -> job.publish(Instant.now()));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = JOBS_CACHE, key = "#publicId"),
            @CacheEvict(cacheNames = JOB_LISTINGS_CACHE, allEntries = true)
    })
    public JobResponseDTO pauseJob(UUID publicId) {
        return mutate(publicId, Job::pause);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = JOBS_CACHE, key = "#publicId"),
            @CacheEvict(cacheNames = JOB_LISTINGS_CACHE, allEntries = true)
    })
    public JobResponseDTO closeJob(UUID publicId, JobStatus closingStatus) {
        return mutate(publicId, job -> job.close(closingStatus, Instant.now()));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = JOBS_CACHE, key = "#publicId"),
            @CacheEvict(cacheNames = JOB_LISTINGS_CACHE, allEntries = true)
    })
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
                dto.getSalaryPeriod(), dto.getSalaryNegotiable());
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
                || dto.getSalaryPeriod() != null || dto.getSalaryNegotiable() != null) {
            job.updateSalary(
                    orElse(dto.getSalaryMin(), job.getSalaryMin()),
                    orElse(dto.getSalaryMax(), job.getSalaryMax()),
                    orElse(dto.getSalaryCurrency(), job.getSalaryCurrency()),
                    orElse(dto.getSalaryPeriod(), job.getSalaryPeriod()),
                    orElse(dto.getSalaryNegotiable(), job.isSalaryNegotiable()));
        }

        replaceSkillsIfPresent(job, dto.getSkills());
    }

    // No salary amount means no salary at all; clearSalary keeps currency/period
    // consistent and avoids validateSalary rejecting a stray currency/period.
    private void applySalary(Job job, Integer salaryMin, Integer salaryMax, SalaryCurrency currency,
                             SalaryPeriod period, Boolean negotiable) {
        if (salaryMin == null && salaryMax == null) {
            job.clearSalary();
            return;
        }
        job.updateSalary(salaryMin, salaryMax, currency, period, Boolean.TRUE.equals(negotiable));
    }

    private static <T> T orElse(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private static int openPositionsOrDefault(Integer openPositions) {
        return openPositions != null ? openPositions : 1;
    }

    private Job findJob(UUID publicId) {
        return jobRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Job with public ID '%s' not found.",
                        publicId)));
    }

    private Department findDepartment(String name) {
        return departmentRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Department with name '%s' not found."
                        , name)));
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
