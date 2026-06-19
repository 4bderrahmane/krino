package com.krino.backend.repository;

import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, Long>
{

    Optional<Job> findByPublicId(UUID publicId);

    List<Job> findByTitle(String title);

    List<Job> findByDepartment(Department department);

    List<Job> findByStatus(JobStatus status);

    List<Job> findByApplyingDeadlineAfter(LocalDate currentDate);

    List<Job> findByStatusAndApplyingDeadlineAfter(JobStatus status, LocalDate currentDate);

    List<Job> findByDepartmentAndApplyingDeadlineAfter(Department department, LocalDate currentDate);

    List<Job> findByDepartmentAndStatusAndApplyingDeadlineAfter(Department department, JobStatus status, LocalDate currentDate);

    List<Job> findByTitleAndApplyingDeadlineAfter(String title, LocalDate currentDate);

    List<Job> findByTitleAndStatusAndApplyingDeadlineAfter(String title, JobStatus status, LocalDate currentDate);

    List<Job> findByTitleAndDepartmentAndApplyingDeadlineAfter(String title, Department department, LocalDate currentDate);

    List<Job> findByTitleAndDepartmentAndStatusAndApplyingDeadlineAfter(String title, Department department, JobStatus status, LocalDate currentDate);

}
