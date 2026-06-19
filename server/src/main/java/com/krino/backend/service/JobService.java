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
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.repository.DepartmentRepository;
import com.krino.backend.repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Transactional
@Service
@RequiredArgsConstructor
public class JobService
{
    private static final String INVALID_EMPLOYMENT_TYPE_MESSAGE = "Employment type '%s' doesn't exist.";
    private static final String INVALID_CONTRACT_TYPE_MESSAGE = "Contract type '%s' doesn't exist.";
    private static final String INVALID_JOB_STATUS_MESSAGE = "Job status '%s' doesn't exist.";
    private static final String JOB_NOT_FOUND_MESSAGE = "Job with public ID '%s' not found.";
    private static final String DEPARTMENT_NOT_FOUND_MESSAGE = "Department with name '%s' not found.";
    private final JobRepository jobRepository;
    private final DepartmentRepository departmentRepository;
    private final ModelMapper modelMapper;

    public void deleteJobByPublicId(UUID publicId)
    {
        Job job = jobRepository.findByPublicId(publicId).orElseThrow(() -> new ResourceNotFoundException(String.format(JOB_NOT_FOUND_MESSAGE, publicId)));

        jobRepository.delete(job);
    }

    public JobResponseDTO createJob(JobCreateDTO dto)
    {
        Department department = departmentRepository.findByName(dto.getDepartmentName()).orElseThrow(() -> new ResourceNotFoundException(String.format(DEPARTMENT_NOT_FOUND_MESSAGE, dto.getDepartmentName())));

        Job job = modelMapper.map(dto, Job.class);

        job.setDepartment(department);
        job.setEmploymentType(parseEnum(EmploymentType.class, dto.getEmploymentType(),
                INVALID_EMPLOYMENT_TYPE_MESSAGE));
        job.setContractType(parseEnum(ContractType.class, dto.getContractType(), INVALID_CONTRACT_TYPE_MESSAGE));

        job.setStatus(JobStatus.OPEN);
        Job savedJob = jobRepository.save(job);
        return modelMapper.map(savedJob, JobResponseDTO.class);
    }

    public JobResponseDTO getJobByPublicId(UUID publicId)
    {
        Job job = jobRepository.findByPublicId(publicId).orElseThrow(() -> new ResourceNotFoundException(String.format(JOB_NOT_FOUND_MESSAGE, publicId)));
        return modelMapper.map(job, JobResponseDTO.class);
    }

    public PageResponse<JobResponseDTO> getAllJobs(Pageable pageable)
    {
        return PageResponse.from(jobRepository.findAll(pageable),
                job -> modelMapper.map(job, JobResponseDTO.class));
    }

    public JobResponseDTO updateJob(UUID publicId, JobUpdateDTO jobUpdateDTO)
    {
        Job existingJob = jobRepository.findByPublicId(publicId).orElseThrow(() -> new ResourceNotFoundException(String.format(JOB_NOT_FOUND_MESSAGE, publicId)));

        updateJobFromDto(existingJob, jobUpdateDTO);

        Job updatedJob = jobRepository.save(existingJob);
        return modelMapper.map(updatedJob, JobResponseDTO.class);
    }

    public JobResponseDTO patchJob(UUID publicId, JobUpdateDTO jobUpdateDTO)
    {
        Job existingJob = jobRepository.findByPublicId(publicId).orElseThrow(() -> new ResourceNotFoundException(String.format(JOB_NOT_FOUND_MESSAGE, publicId)));

        patchJobFromDto(existingJob, jobUpdateDTO);

        Job updatedJob = jobRepository.save(existingJob);
        return modelMapper.map(updatedJob, JobResponseDTO.class);
    }

    private void updateJobFromDto(Job job, JobUpdateDTO dto)
    {
        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());

        if (dto.getDepartmentName() != null)
        {
            Department department = departmentRepository
                    .findByName(dto.getDepartmentName())
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(DEPARTMENT_NOT_FOUND_MESSAGE, dto.getDepartmentName())));
            job.setDepartment(department);
        }

        job.setEmploymentType(parseEnum(EmploymentType.class, dto.getEmploymentType(),
                INVALID_EMPLOYMENT_TYPE_MESSAGE));
        job.setContractType(parseEnum(ContractType.class, dto.getContractType(), INVALID_CONTRACT_TYPE_MESSAGE));
        job.setStatus(parseEnum(JobStatus.class, dto.getStatus(), INVALID_JOB_STATUS_MESSAGE));
    }

    private void patchJobFromDto(Job job, JobUpdateDTO dto)
    {
        if (dto.getTitle() != null)
        {
            job.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null)
        {
            job.setDescription(dto.getDescription());
        }
        if (dto.getDepartmentName() != null)
        {
            Department department = departmentRepository.findByName(dto.getDepartmentName()).orElseThrow(() -> new ResourceNotFoundException(String.format(DEPARTMENT_NOT_FOUND_MESSAGE, dto.getDepartmentName())));
            job.setDepartment(department);
        }
        if (dto.getEmploymentType() != null)
        {
            job.setEmploymentType(parseEnum(EmploymentType.class, dto.getEmploymentType(),
                    INVALID_EMPLOYMENT_TYPE_MESSAGE));
        }
        if (dto.getContractType() != null)
        {
            job.setContractType(parseEnum(ContractType.class, dto.getContractType(),
                    INVALID_CONTRACT_TYPE_MESSAGE));
        }
        if (dto.getStatus() != null)
        {
            job.setStatus(parseEnum(JobStatus.class, dto.getStatus(), INVALID_JOB_STATUS_MESSAGE));
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String invalidMessage)
    {
        try
        {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e)
        {
            throw new InvalidJobTypeException(String.format(invalidMessage, value));
        }
    }
}
