package com.jesa.interviewslotmanager.service;

import com.jesa.interviewslotmanager.dto.job.JobCreateDTO;
import com.jesa.interviewslotmanager.entity.Department;
import com.jesa.interviewslotmanager.entity.Job;
import com.jesa.interviewslotmanager.exception.DepartmentNotFoundException;
import com.jesa.interviewslotmanager.exception.InvalidJobTypeException;
import com.jesa.interviewslotmanager.exception.JobNotFoundException;
import com.jesa.interviewslotmanager.repository.DepartmentRepository;
import com.jesa.interviewslotmanager.repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Transactional
@Service
@RequiredArgsConstructor
public class JobService
{
    private static final String INVALID_JOB_TYPE_MESSAGE = "Job type '%s' doesn't exist.";
    private final JobRepository jobRepository;
    private final DepartmentRepository departmentRepository;
    private final ModelMapper modelMapper;

    public void deleteJobById(Long jobId)
    {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("ID", jobId));

        jobRepository.delete(job);
    }

    public Job createJob(JobCreateDTO dto)
    {
        Department department = departmentRepository.findByName(dto.getDepartmentName())
                .orElseThrow(() -> new DepartmentNotFoundException("Name", dto.getDepartmentName()));

        Job job = modelMapper.map(dto, Job.class);

        job.setDepartment(department);
        try
        {
            job.setType(Job.JobType.valueOf(dto.getType()));
        } catch (IllegalArgumentException e)
        {
            throw new InvalidJobTypeException(String.format(INVALID_JOB_TYPE_MESSAGE, dto.getType()));
        }

        job.setStatus(Job.JobStatus.OPEN);
        return jobRepository.save(job);
    }
}