package com.krino.backend.mapper;

import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.JobSkill;
import com.krino.backend.entity.Skill;
import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.ExperienceLevel;
import com.krino.backend.entity.enums.JobStatus;
import com.krino.backend.entity.enums.MoroccanCity;
import com.krino.backend.entity.enums.RemotePolicy;
import com.krino.backend.entity.enums.SalaryCurrency;
import com.krino.backend.entity.enums.SalaryPeriod;
import com.krino.backend.entity.enums.SkillImportance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JobMapperTest {
    private JobMapper jobMapper;

    @BeforeEach
    void setUp() {
        jobMapper = Mappers.getMapper(JobMapper.class);
        // The Spring component model leaves used-mappers unset when built via the
        // standalone factory, so wire DepartmentMapper in for toResponse.
        ReflectionTestUtils.setField(jobMapper, "departmentMapper", Mappers.getMapper(DepartmentMapper.class));
    }

    @Test
    void toResponse_rendersScalarFields() {
        Instant deadline = Instant.parse("2099-12-31T23:59:00Z");

        Job job = new Job("JOB-TEST", "backend-engineer", new Department(), "Backend Engineer",
                EmploymentType.FULL_TIME, ContractType.PERMANENT, RemotePolicy.HYBRID);
        job.updateContent("Backend Engineer", "Build APIs");
        job.updateClassification(EmploymentType.FULL_TIME, ContractType.PERMANENT,
                ExperienceLevel.MID_LEVEL, 3, 2);
        job.updateWorkArrangement(RemotePolicy.HYBRID, MoroccanCity.CASABLANCA);
        job.updateTimeline(deadline, null);
        job.updateSalary(12000, 18000, SalaryCurrency.MAD, SalaryPeriod.MONTHLY, false);

        JobResponseDTO response = jobMapper.toResponse(job);

        assertEquals("Backend Engineer", response.getTitle());
        assertEquals("Build APIs", response.getDescription());
        assertEquals(deadline, response.getApplicationDeadline());
        assertEquals(Integer.valueOf(12000), response.getSalaryMin());
        assertEquals(Integer.valueOf(18000), response.getSalaryMax());
        assertEquals(SalaryCurrency.MAD, response.getSalaryCurrency());
        assertEquals(SalaryPeriod.MONTHLY, response.getSalaryPeriod());
        assertEquals(MoroccanCity.CASABLANCA, response.getCity());
        assertEquals(RemotePolicy.HYBRID, response.getRemotePolicy());
        assertEquals(ExperienceLevel.MID_LEVEL, response.getExperienceLevel());
        assertEquals(2, response.getOpenPositions());
        // Freshly built postings start as drafts; publication is a separate action.
        assertEquals(JobStatus.DRAFT, response.getStatus());
    }

    @Test
    void mapSkills_returnsStableRequiredFirstOrder() {
        Job job = new Job("JOB-TEST", "skills", new Department(), "Backend Engineer",
                EmploymentType.FULL_TIME, ContractType.PERMANENT, RemotePolicy.REMOTE);
        job.addSkill(jobSkill(skill("React", "react"), SkillImportance.PREFERRED));
        job.addSkill(jobSkill(skill("Java", "java"), SkillImportance.REQUIRED));

        assertThat(jobMapper.mapSkills(job.getSkills()))
                .extracting("slug", "importance")
                .containsExactly(
                        tuple("java", SkillImportance.REQUIRED),
                        tuple("react", SkillImportance.PREFERRED));
    }

    private JobSkill jobSkill(Skill skill, SkillImportance importance) {
        JobSkill jobSkill = new JobSkill();
        jobSkill.setSkill(skill);
        jobSkill.setImportance(importance);
        return jobSkill;
    }

    private Skill skill(String name, String slug) {
        Skill skill = new Skill();
        skill.setName(name);
        skill.setSlug(slug);
        return skill;
    }
}
