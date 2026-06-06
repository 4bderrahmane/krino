package com.krino.backend.service;

import com.krino.backend.dto.job.JobCreateDTO;
import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.dto.job.JobUpdateDTO;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.exception.InvalidJobTypeException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.repository.DepartmentRepository;
import com.krino.backend.repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Transactional
@Service
@RequiredArgsConstructor
public class JobService
{
    private static final String INVALID_JOB_TYPE_MESSAGE = "Job type '%s' doesn't exist.";
    private static final String INVALID_JOB_STATUS_MESSAGE = "Job status '%s' doesn't exist.";
    private static final String JOB_NOT_FOUND_MESSAGE = "Job with ID '%d' not found.";
    private static final String DEPARTMENT_NOT_FOUND_MESSAGE = "Department with name '%s' not found.";
    private final JobRepository jobRepository;
    private final DepartmentRepository departmentRepository;
    private final ModelMapper modelMapper;

    public void deleteJobById(Long jobId)
    {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(String.format(JOB_NOT_FOUND_MESSAGE, jobId)));

        jobRepository.delete(job);
    }

    public JobResponseDTO createJob(JobCreateDTO dto)
    {
        Department department = departmentRepository.findByName(dto.getDepartmentName()).orElseThrow(() -> new ResourceNotFoundException(String.format(DEPARTMENT_NOT_FOUND_MESSAGE, dto.getDepartmentName())));

        Job job = modelMapper.map(dto, Job.class);

        job.setDepartment(department);
        try
        {
            job.setType(Job.JobType.valueOf(dto.getType().toUpperCase()));
        } catch (IllegalArgumentException e)
        {
            throw new InvalidJobTypeException(String.format(INVALID_JOB_TYPE_MESSAGE, dto.getType()));
        }

        job.setStatus(Job.JobStatus.OPEN);
        Job savedJob = jobRepository.save(job);
        return modelMapper.map(savedJob, JobResponseDTO.class);
    }

    public JobResponseDTO getJobById(Long jobId)
    {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(String.format(JOB_NOT_FOUND_MESSAGE, jobId)));
        return modelMapper.map(job, JobResponseDTO.class);
    }

    public List<JobResponseDTO> getAllJobs()
    {
        return jobRepository.findAll().stream()
                .map(job -> modelMapper.map(job, JobResponseDTO.class))
                .toList();
    }

    public JobResponseDTO updateJob(Long jobId, JobUpdateDTO jobUpdateDTO)
    {
        Job existingJob = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(String.format(JOB_NOT_FOUND_MESSAGE, jobId)));

        updateJobFromDto(existingJob, jobUpdateDTO);

        Job updatedJob = jobRepository.save(existingJob);
        return modelMapper.map(updatedJob, JobResponseDTO.class);
    }

    public JobResponseDTO patchJob(Long jobId, JobUpdateDTO jobUpdateDTO)
    {
        Job existingJob = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException(String.format(JOB_NOT_FOUND_MESSAGE, jobId)));

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

        try
        {
            job.setType(Job.JobType.valueOf(dto.getType().toUpperCase()));
        } catch (IllegalArgumentException e)
        {
            throw new InvalidJobTypeException(String.format(INVALID_JOB_TYPE_MESSAGE, dto.getType()));
        }

        try
        {
            job.setStatus(Job.JobStatus.valueOf(dto.getStatus().toUpperCase()));
        } catch (IllegalArgumentException e)
        {
            throw new InvalidJobTypeException(String.format(INVALID_JOB_STATUS_MESSAGE, dto.getStatus()));
        }
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
        if (dto.getType() != null)
        {
            try
            {
                job.setType(Job.JobType.valueOf(dto.getType().toUpperCase()));
            } catch (IllegalArgumentException e)
            {
                throw new InvalidJobTypeException(String.format(INVALID_JOB_TYPE_MESSAGE, dto.getType()));
            }
        }
        if (dto.getStatus() != null)
        {
            try
            {
                job.setStatus(Job.JobStatus.valueOf(dto.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e)
            {
                throw new InvalidJobTypeException(String.format(INVALID_JOB_STATUS_MESSAGE, dto.getStatus()));
            }
        }
    }
}