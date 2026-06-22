package com.krino.backend.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobServiceTest
{
    private JobRepository jobRepository;
    private DepartmentRepository departmentRepository;
    private ApplicationRepository applicationRepository;
    private InterviewRepository interviewRepository;
    private JobMapper jobMapper;
    private JobService jobService;

    @BeforeEach
    void setUp()
    {
        jobRepository = mock(JobRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        applicationRepository = mock(ApplicationRepository.class);
        interviewRepository = mock(InterviewRepository.class);
        jobMapper = mock(JobMapper.class);
        jobService = new JobService(jobRepository, departmentRepository, applicationRepository, interviewRepository,
                jobMapper);
    }

    @Test
    void createJob_validRequest_resolvesDepartmentAndEnumsThenSaves()
    {
        JobCreateDTO dto = createDto("Engineering", "FULL_TIME", "PERMANENT");
        Department department = new Department();
        Job entity = new Job();
        Job saved = new Job();
        JobResponseDTO response = new JobResponseDTO();

        when(departmentRepository.findByName("Engineering")).thenReturn(Optional.of(department));
        when(jobMapper.toEntity(dto, department, EmploymentType.FULL_TIME, ContractType.PERMANENT, JobStatus.OPEN))
                .thenReturn(entity);
        when(jobRepository.save(entity)).thenReturn(saved);
        when(jobMapper.toResponse(saved)).thenReturn(response);

        JobResponseDTO result = jobService.createJob(dto);

        assertThat(result).isSameAs(response);
        verify(jobRepository).save(entity);
    }

    @Test
    void createJob_unknownDepartment_throwsResourceNotFound()
    {
        JobCreateDTO dto = createDto("Ghost", "FULL_TIME", "PERMANENT");
        when(departmentRepository.findByName("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.createJob(dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobRepository, never()).save(any());
    }

    @Test
    void createJob_invalidEmploymentType_throwsInvalidJobType()
    {
        JobCreateDTO dto = createDto("Engineering", "NOT_A_TYPE", "PERMANENT");
        when(departmentRepository.findByName("Engineering")).thenReturn(Optional.of(new Department()));

        assertThatThrownBy(() -> jobService.createJob(dto))
                .isInstanceOf(InvalidJobTypeException.class);

        verify(jobRepository, never()).save(any());
    }

    @Test
    void createJob_invalidContractType_throwsInvalidJobType()
    {
        JobCreateDTO dto = createDto("Engineering", "FULL_TIME", "NOT_A_CONTRACT");
        when(departmentRepository.findByName("Engineering")).thenReturn(Optional.of(new Department()));

        assertThatThrownBy(() -> jobService.createJob(dto))
                .isInstanceOf(InvalidJobTypeException.class);

        verify(jobRepository, never()).save(any());
    }

    @Test
    void createJob_lowercaseEnumValues_areAcceptedCaseInsensitively()
    {
        JobCreateDTO dto = createDto("Engineering", "full_time", "permanent");
        Department department = new Department();

        when(departmentRepository.findByName("Engineering")).thenReturn(Optional.of(department));
        when(jobMapper.toEntity(eq(dto), eq(department), eq(EmploymentType.FULL_TIME), eq(ContractType.PERMANENT),
                eq(JobStatus.OPEN))).thenReturn(new Job());
        when(jobRepository.save(any())).thenReturn(new Job());

        jobService.createJob(dto);

        verify(jobMapper).toEntity(dto, department, EmploymentType.FULL_TIME, ContractType.PERMANENT, JobStatus.OPEN);
    }

    @Test
    void getJobByPublicId_unknown_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(jobRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobByPublicId(publicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateJob_unknown_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(jobRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.updateJob(publicId, new JobUpdateDTO()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobRepository, never()).save(any());
    }

    @Test
    void deleteJob_unknown_throwsResourceNotFound()
    {
        UUID publicId = UUID.randomUUID();
        when(jobRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.deleteJobByPublicId(publicId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobRepository, never()).delete(any());
    }

    @Test
    void deleteJob_withApplications_throwsConflictAndDoesNotDelete()
    {
        UUID publicId = UUID.randomUUID();
        Job job = new Job();
        job.setTitle("Backend Engineer");

        when(jobRepository.findByPublicId(publicId)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByJob(job)).thenReturn(true);

        assertThatThrownBy(() -> jobService.deleteJobByPublicId(publicId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("cannot be deleted");

        verify(jobRepository, never()).delete(any());
    }

    @Test
    void deleteJob_withoutApplicationsOrInterviews_deletesJob()
    {
        UUID publicId = UUID.randomUUID();
        Job job = new Job();
        job.setTitle("Backend Engineer");

        when(jobRepository.findByPublicId(publicId)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByJob(job)).thenReturn(false);
        when(interviewRepository.existsByJob(job)).thenReturn(false);

        jobService.deleteJobByPublicId(publicId);

        verify(jobRepository).delete(job);
    }

    private JobCreateDTO createDto(String department, String employmentType, String contractType)
    {
        JobCreateDTO dto = new JobCreateDTO();
        dto.setDepartmentName(department);
        dto.setTitle("Backend Engineer");
        dto.setEmploymentType(employmentType);
        dto.setContractType(contractType);
        return dto;
    }
}
