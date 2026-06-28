package com.krino.backend.service;

import com.krino.backend.dto.job.JobCreateDTO;
import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.dto.job.JobSkillRequestDTO;
import com.krino.backend.dto.job.JobUpdateDTO;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.Skill;
import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.JobStatus;
import com.krino.backend.entity.enums.RemotePolicy;
import com.krino.backend.entity.enums.SkillImportance;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.JobMapper;
import com.krino.backend.repository.ApplicationRepository;
import com.krino.backend.repository.DepartmentRepository;
import com.krino.backend.repository.JobRepository;
import com.krino.backend.repository.SkillRepository;
import com.krino.backend.support.TestJobs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobServiceTest
{
    private JobRepository jobRepository;
    private DepartmentRepository departmentRepository;
    private ApplicationRepository applicationRepository;
    private SkillRepository skillRepository;
    private JobMapper jobMapper;
    private JobService jobService;

    @BeforeEach
    void setUp()
    {
        jobRepository = mock(JobRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        applicationRepository = mock(ApplicationRepository.class);
        skillRepository = mock(SkillRepository.class);
        jobMapper = mock(JobMapper.class);
        jobService = new JobService(jobRepository, departmentRepository, applicationRepository, skillRepository,
                jobMapper);
    }

    @Test
    void createJob_validRequest_buildsDraftAndSaves()
    {
        JobCreateDTO dto = createDto("Engineering", EmploymentType.FULL_TIME, ContractType.PERMANENT);
        Department department = new Department();
        JobResponseDTO response = new JobResponseDTO();

        when(departmentRepository.findByName("Engineering")).thenReturn(Optional.of(department));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobMapper.toResponse(any(Job.class))).thenReturn(response);

        JobResponseDTO result = jobService.createJob(dto);

        assertThat(result).isSameAs(response);

        Job saved = capturedSavedJob();
        assertThat(saved.getTitle()).isEqualTo("Backend Engineer");
        assertThat(saved.getDepartment()).isSameAs(department);
        // Postings are created as drafts; publication is an explicit action.
        assertThat(saved.getStatus()).isEqualTo(JobStatus.DRAFT);
    }

    @Test
    void createJob_withSkills_reusesSkillsAndAttachesImportance()
    {
        JobCreateDTO dto = createDto("Engineering", EmploymentType.FULL_TIME, ContractType.PERMANENT);
        dto.setSkills(List.of(skillRequest("Java", SkillImportance.REQUIRED),
                skillRequest("Spring Boot", SkillImportance.PREFERRED)));
        Department department = new Department();
        JobResponseDTO response = new JobResponseDTO();
        Skill java = skill("Java", "java");
        Skill springBoot = skill("Spring Boot", "spring-boot");

        when(departmentRepository.findByName("Engineering")).thenReturn(Optional.of(department));
        when(skillRepository.findBySlug("java")).thenReturn(Optional.of(java));
        when(skillRepository.findBySlug("spring-boot")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenReturn(springBoot);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobMapper.toResponse(any(Job.class))).thenReturn(response);

        assertThat(jobService.createJob(dto)).isSameAs(response);

        Job saved = capturedSavedJob();
        assertThat(saved.getSkills())
                .hasSize(2)
                .allSatisfy(jobSkill -> assertThat(jobSkill.getJob()).isSameAs(saved));
        assertThat(saved.getSkills())
                .extracting(jobSkill -> jobSkill.getSkill().getSlug(), jobSkill -> jobSkill.getImportance())
                .containsExactlyInAnyOrder(
                        tuple("java", SkillImportance.REQUIRED),
                        tuple("spring-boot", SkillImportance.PREFERRED));
    }

    @Test
    void createJob_withDuplicateSkillSlugs_throwsConflict()
    {
        JobCreateDTO dto = createDto("Engineering", EmploymentType.FULL_TIME, ContractType.PERMANENT);
        dto.setSkills(List.of(skillRequest("C#", SkillImportance.REQUIRED),
                skillRequest("C sharp", SkillImportance.PREFERRED)));
        Department department = new Department();
        Skill cSharp = skill("C#", "c-sharp");

        when(departmentRepository.findByName("Engineering")).thenReturn(Optional.of(department));
        when(skillRepository.findBySlug("c-sharp")).thenReturn(Optional.of(cSharp));

        assertThatThrownBy(() -> jobService.createJob(dto))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Duplicate skill");

        verify(jobRepository, never()).save(any());
    }

    @Test
    void createJob_unknownDepartment_throwsResourceNotFound()
    {
        JobCreateDTO dto = createDto("Ghost", EmploymentType.FULL_TIME, ContractType.PERMANENT);
        when(departmentRepository.findByName("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.createJob(dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobRepository, never()).save(any());
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

        JobUpdateDTO update = new JobUpdateDTO();
        assertThatThrownBy(() -> jobService.updateJob(publicId, update))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobRepository, never()).save(any());
    }

    @Test
    void pauseJob_draftPosting_throwsConflict()
    {
        UUID publicId = UUID.randomUUID();
        // A DRAFT cannot be paused; the entity raises an invalid-transition error
        // that the service must surface as a conflict, not a 500.
        when(jobRepository.findByPublicId(publicId)).thenReturn(Optional.of(TestJobs.draft("Backend Engineer")));

        assertThatThrownBy(() -> jobService.pauseJob(publicId))
                .isInstanceOf(ResourceConflictException.class);

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
        Job job = TestJobs.draft("Backend Engineer");

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
        Job job = TestJobs.draft("Backend Engineer");

        when(jobRepository.findByPublicId(publicId)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByJob(job)).thenReturn(false);

        jobService.deleteJobByPublicId(publicId);

        verify(jobRepository).delete(job);
    }

    private Job capturedSavedJob()
    {
        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        return captor.getValue();
    }

    private JobCreateDTO createDto(String department, EmploymentType employmentType, ContractType contractType)
    {
        JobCreateDTO dto = new JobCreateDTO();
        dto.setDepartmentName(department);
        dto.setTitle("Backend Engineer");
        dto.setEmploymentType(employmentType);
        dto.setContractType(contractType);
        dto.setRemotePolicy(RemotePolicy.REMOTE);
        return dto;
    }

    private JobSkillRequestDTO skillRequest(String name, SkillImportance importance)
    {
        JobSkillRequestDTO request = new JobSkillRequestDTO();
        request.setName(name);
        request.setImportance(importance);
        return request;
    }

    private Skill skill(String name, String slug)
    {
        Skill skill = new Skill();
        skill.setName(name);
        skill.setSlug(slug);
        return skill;
    }
}
