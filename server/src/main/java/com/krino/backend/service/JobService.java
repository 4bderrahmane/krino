package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.job.JobCreateDTO;
import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.dto.job.JobUpdateDTO;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.JobStatus;
import com.krino.backend.exception.InvalidJobTypeException;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.JobMapper;
import com.krino.backend.repository.ApplicationRepository;
import com.krino.backend.repository.DepartmentRepository;
import com.krino.backend.repository.InterviewRepository;
import com.krino.backend.repository.JobRepository;
import com.krino.backend.utility.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Transactional
@Service
@RequiredArgsConstructor
public class JobService {
    private static final String INVALID_EMPLOYMENT_TYPE_MESSAGE = "Employment type '%s' doesn't exist.";
    private static final String INVALID_CONTRACT_TYPE_MESSAGE = "Contract type '%s' doesn't exist.";
    private static final String INVALID_JOB_STATUS_MESSAGE = "Job status '%s' doesn't exist.";
    private static final String JOB_NOT_FOUND_MESSAGE = "Job with public ID '%s' not found.";
    private static final String DEPARTMENT_NOT_FOUND_MESSAGE = "Department with name '%s' not found.";
    private final JobRepository jobRepository;
    private final DepartmentRepository departmentRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final JobMapper jobMapper;

    public void deleteJobByPublicId(UUID publicId) {
        Job job = jobRepository.findByPublicId(publicId).orElseThrow(() -> new ResourceNotFoundException(String.format(JOB_NOT_FOUND_MESSAGE, publicId)));

        if (applicationRepository.existsByJob(job) || interviewRepository.existsByJob(job)) {
            throw new ResourceConflictException(
                    String.format("Job '%s' has applications or interviews and cannot be deleted; close it instead.",
                            job.getTitle()),
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    Map.of("resource", "Job", "title", job.getTitle()));
        }

        jobRepository.delete(job);
    }

    public JobResponseDTO createJob(JobCreateDTO dto) {
        Department department =
                departmentRepository.findByName(dto.getDepartmentName()).orElseThrow(() -> new ResourceNotFoundException(String.format(DEPARTMENT_NOT_FOUND_MESSAGE, dto.getDepartmentName())));

        EmploymentType employmentType = parseEnum(EmploymentType.class, dto.getEmploymentType(),
                INVALID_EMPLOYMENT_TYPE_MESSAGE);
        ContractType contractType = parseEnum(ContractType.class, dto.getContractType(),
                INVALID_CONTRACT_TYPE_MESSAGE);

        Job job = jobMapper.toEntity(dto, department, employmentType, contractType, JobStatus.OPEN);
        Job savedJob = jobRepository.save(job);
        return jobMapper.toResponse(savedJob);
    }

    public JobResponseDTO getJobByPublicId(UUID publicId) {
        Job job =
                jobRepository.findByPublicId(publicId).orElseThrow(() -> new ResourceNotFoundException(String.format(JOB_NOT_FOUND_MESSAGE, publicId)));
        return jobMapper.toResponse(job);
    }

    public PageResponse<JobResponseDTO> getAllJobs(Pageable pageable) {
        return PageResponse.from(jobRepository.findAll(pageable),
                jobMapper::toResponse);
    }

    public JobResponseDTO updateJob(UUID publicId, JobUpdateDTO jobUpdateDTO) {
        Job existingJob =
                jobRepository.findByPublicId(publicId).orElseThrow(() -> new ResourceNotFoundException(String.format(JOB_NOT_FOUND_MESSAGE, publicId)));

        updateJobFromDto(existingJob, jobUpdateDTO);

        Job updatedJob = jobRepository.save(existingJob);
        return jobMapper.toResponse(updatedJob);
    }

    public JobResponseDTO patchJob(UUID publicId, JobUpdateDTO jobUpdateDTO) {
        Job existingJob =
                jobRepository.findByPublicId(publicId).orElseThrow(() -> new ResourceNotFoundException(String.format(JOB_NOT_FOUND_MESSAGE, publicId)));

        patchJobFromDto(existingJob, jobUpdateDTO);

        Job updatedJob = jobRepository.save(existingJob);
        return jobMapper.toResponse(updatedJob);
    }

    private void updateJobFromDto(Job job, JobUpdateDTO dto) {
        Department department = null;

        if (dto.getDepartmentName() != null) {
            department = departmentRepository
                    .findByName(dto.getDepartmentName())
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(DEPARTMENT_NOT_FOUND_MESSAGE,
                            dto.getDepartmentName())));
        }

        EmploymentType employmentType = parseEnum(EmploymentType.class, dto.getEmploymentType(),
                INVALID_EMPLOYMENT_TYPE_MESSAGE);
        ContractType contractType = parseEnum(ContractType.class, dto.getContractType(), INVALID_CONTRACT_TYPE_MESSAGE);
        JobStatus status = parseEnum(JobStatus.class, dto.getStatus(), INVALID_JOB_STATUS_MESSAGE);

        jobMapper.updateEntity(dto, department, employmentType, contractType, status, job);
    }

    private void patchJobFromDto(Job job, JobUpdateDTO dto) {
        Department department = null;
        EmploymentType employmentType = null;
        ContractType contractType = null;
        JobStatus status = null;

        if (dto.getDepartmentName() != null) {
            department =
                    departmentRepository.findByName(dto.getDepartmentName()).orElseThrow(() -> new ResourceNotFoundException(String.format(DEPARTMENT_NOT_FOUND_MESSAGE, dto.getDepartmentName())));
        }
        if (dto.getEmploymentType() != null) {
            employmentType = parseEnum(EmploymentType.class, dto.getEmploymentType(),
                    INVALID_EMPLOYMENT_TYPE_MESSAGE);
        }
        if (dto.getContractType() != null) {
            contractType = parseEnum(ContractType.class, dto.getContractType(),
                    INVALID_CONTRACT_TYPE_MESSAGE);
        }
        if (dto.getStatus() != null) {
            status = parseEnum(JobStatus.class, dto.getStatus(), INVALID_JOB_STATUS_MESSAGE);
        }

        jobMapper.patchEntity(dto, department, employmentType, contractType, status, job);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String invalidMessage) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidJobTypeException(String.format(invalidMessage, value));
        }
    }
}
